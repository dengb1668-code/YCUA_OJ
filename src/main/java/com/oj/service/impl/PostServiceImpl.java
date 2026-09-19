package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.NicknameUtil;
import com.oj.common.UserContext;
import com.oj.dto.PostCreateRequest;
import com.oj.dto.PostUpdateRequest;
import com.oj.entity.Post;
import com.oj.entity.Problem;
import com.oj.entity.Reply;
import com.oj.entity.User;
import com.oj.enums.PostType;
import com.oj.mapper.PostMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.ReplyMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.PermissionService;
import com.oj.service.PostService;
import com.oj.vo.PostDetailVO;
import com.oj.vo.PostListVO;
import com.oj.vo.ReplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final ReplyMapper replyMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final PermissionService permissionService;

    @Override
    public Page<PostListVO> pagePosts(PostType type, Long problemId, long pageNum, long pageSize) {
        Page<Post> page = lambdaQuery()
                .eq(Post::getType, type)
                .eq(problemId != null, Post::getProblemId, problemId)
                .orderByDesc(Post::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        // 3.5.17 中 Page.convert 返回 IPage 而非 Page, 手动构造以保持返回类型
        Page<PostListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Post> posts = page.getRecords();
        List<PostListVO> records = posts.stream().map(p -> {
            PostListVO vo = new PostListVO();
            vo.setId(p.getId());
            vo.setType(p.getType());
            vo.setTitle(p.getTitle());
            vo.setProblemId(p.getProblemId());
            vo.setAuthorId(p.getUserId());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(records);

        // 批量补齐: 题目标题 / 作者昵称 / 回复数
        List<Long> problemIds = posts.stream().map(Post::getProblemId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!problemIds.isEmpty()) {
            Map<Long, Problem> problemMap = problemMapper.selectBatchIds(problemIds).stream()
                    .collect(Collectors.toMap(Problem::getId, Function.identity()));
            records.forEach(vo -> {
                Problem p = problemMap.get(vo.getProblemId());
                if (p != null) {
                    vo.setProblemTitle(p.getTitle());
                }
            });
        }
        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            records.forEach(vo -> {
                User u = userMap.get(vo.getAuthorId());
                vo.setAuthorName(u == null ? "未知用户" : nicknameOf(u));
            });
        }
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        if (!postIds.isEmpty()) {
            Map<Long, Long> replyCountMap = replyMapper.selectMaps(new QueryWrapper<Reply>()
                            .select("post_id", "COUNT(*) AS cnt")
                            .in("post_id", postIds)
                            .groupBy("post_id")).stream()
                    .collect(Collectors.toMap(m -> ((Number) m.get("post_id")).longValue(),
                            m -> ((Number) m.get("cnt")).longValue()));
            records.forEach(vo -> vo.setReplyCount(replyCountMap.getOrDefault(vo.getId(), 0L)));
        } else {
            records.forEach(vo -> vo.setReplyCount(0L));
        }
        return voPage;
    }

    @Override
    public PostDetailVO detail(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在: id=" + id);
        }
        Long currentUserId = UserContext.getUserId();
        PostDetailVO vo = new PostDetailVO();
        vo.setId(post.getId());
        vo.setType(post.getType());
        vo.setTitle(post.getTitle());
        vo.setProblemId(post.getProblemId());
        vo.setAuthorId(post.getUserId());
        vo.setContent(post.getContent());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        // 作者本人或管理端可编辑/删除
        boolean manager = permissionService.isCurrentManager();
        boolean isAuthor = post.getUserId().equals(currentUserId);
        vo.setCanEdit(isAuthor);
        vo.setCanDelete(isAuthor || manager);

        User author = userMapper.selectById(post.getUserId());
        vo.setAuthorName(author == null ? "未知用户" : nicknameOf(author));
        if (post.getProblemId() != null) {
            Problem problem = problemMapper.selectById(post.getProblemId());
            vo.setProblemTitle(problem == null ? "题目已不存在" : problem.getTitle());
        }

        List<Reply> replies = replyMapper.selectList(new LambdaQueryWrapper<Reply>()
                .eq(Reply::getPostId, id)
                .orderByAsc(Reply::getId));
        List<Long> replyUserIds = replies.stream().map(Reply::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = new HashMap<>();
        if (!replyUserIds.isEmpty()) {
            userMapper.selectBatchIds(replyUserIds).forEach(u -> nameMap.put(u.getId(), nicknameOf(u)));
        }
        vo.setReplies(replies.stream().map(r -> {
            ReplyVO rv = new ReplyVO();
            rv.setId(r.getId());
            rv.setUserId(r.getUserId());
            rv.setAuthorName(nameMap.getOrDefault(r.getUserId(), "未知用户"));
            rv.setContent(r.getContent());
            rv.setCanDelete(r.getUserId().equals(currentUserId) || manager);
            rv.setCreateTime(r.getCreateTime());
            return rv;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public Long create(PostCreateRequest request) {
        if (request.getType() == PostType.SOLUTION && request.getProblemId() == null) {
            throw new IllegalArgumentException("题解必须关联题目");
        }
        if (request.getProblemId() != null && problemMapper.selectById(request.getProblemId()) == null) {
            throw new IllegalArgumentException("关联题目不存在: id=" + request.getProblemId());
        }
        Post post = new Post();
        post.setType(request.getType());
        post.setProblemId(request.getProblemId());
        post.setUserId(UserContext.getUserId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        save(post);
        return post.getId();
    }

    @Override
    public void update(Long id, PostUpdateRequest request) {
        Post post = getById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在: id=" + id);
        }
        permissionService.requireAuthorOrManager(post.getUserId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        updateById(post);
    }

    @Override
    public void delete(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在: id=" + id);
        }
        permissionService.requireAuthorOrManager(post.getUserId());
        removeById(id);
        replyMapper.delete(new LambdaQueryWrapper<Reply>().eq(Reply::getPostId, id));
    }

    @Override
    public Long addReply(Long postId, String content) {
        if (getById(postId) == null) {
            throw new IllegalArgumentException("帖子不存在: id=" + postId);
        }
        Reply reply = new Reply();
        reply.setPostId(postId);
        reply.setUserId(UserContext.getUserId());
        reply.setContent(content);
        replyMapper.insert(reply);
        return reply.getId();
    }

    @Override
    public void deleteReply(Long replyId) {
        Reply reply = replyMapper.selectById(replyId);
        if (reply == null) {
            throw new IllegalArgumentException("回复不存在: id=" + replyId);
        }
        permissionService.requireAuthorOrManager(reply.getUserId());
        replyMapper.deleteById(replyId);
    }

    /** 展示名: 优先昵称, 为空回退用户名(公共工具) */
    private String nicknameOf(User user) {
        return NicknameUtil.nicknameOf(user);
    }
}
