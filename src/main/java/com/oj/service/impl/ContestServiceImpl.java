package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.JwtUtil;
import com.oj.common.NicknameUtil;
import com.oj.common.UserContext;
import com.oj.dto.ContestCreateRequest;
import com.oj.dto.ContestJoinRequest;
import com.oj.dto.ContestProblemUpdateRequest;
import com.oj.dto.ContestUpdateRequest;
import com.oj.entity.Contest;
import com.oj.entity.ContestProblem;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.User;
import com.oj.enums.ContestType;
import com.oj.enums.JudgeStatus;
import com.oj.mapper.ContestMapper;
import com.oj.mapper.ContestProblemMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubmissionMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.ContestService;
import com.oj.service.PermissionService;
import com.oj.vo.ContestDetailVO;
import com.oj.vo.ContestJoinVO;
import com.oj.vo.ContestListVO;
import com.oj.vo.ContestProblemVO;
import com.oj.vo.StandingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest> implements ContestService {

    private final ContestProblemMapper contestProblemMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    // ---------- 工具 ----------

    /** 状态: 未开始/进行中/已结束 */
    private String statusOf(Contest contest) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getStartTime())) {
            return "NOT_STARTED";
        }
        if (now.isAfter(contest.getEndTime())) {
            return "ENDED";
        }
        return "RUNNING";
    }

    /** 0->A, 25->Z, 26->AA ... */
    private String toLetters(int i) {
        StringBuilder sb = new StringBuilder();
        int n = i;
        while (true) {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
            if (n < 0) {
                break;
            }
        }
        return sb.toString();
    }

    private boolean canManage(Contest contest) {
        Long current = UserContext.getUserId();
        return contest.getCreatorId().equals(current) || permissionService.isCurrentManager();
    }

    /** 校验比赛访问权限: 创建者/管理端放行; 否则 token 必须为该比赛的访问 token */
    private void requireContestAccess(Contest contest, String contestToken) {
        if (canManage(contest)) {
            return;
        }
        if (!StringUtils.hasText(contestToken)) {
            throw new IllegalArgumentException("请先加入比赛");
        }
        try {
            Long cid = jwtUtil.parseContestToken(contestToken);
            if (!contest.getId().equals(cid)) {
                throw new IllegalArgumentException("比赛访问令牌不匹配");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("比赛访问令牌无效或已过期");
        }
    }

    private List<ContestProblemVO> buildProblemList(Long contestId) {
        List<ContestProblem> cps = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getSort));
        if (cps.isEmpty()) {
            return List.of();
        }
        List<Long> problemIds = cps.stream().map(ContestProblem::getProblemId).toList();
        Map<Long, Problem> problemMap = problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));
        List<ContestProblemVO> result = new ArrayList<>();
        for (ContestProblem cp : cps) {
            Problem p = problemMap.get(cp.getProblemId());
            if (p == null) {
                continue;
            }
            ContestProblemVO vo = new ContestProblemVO();
            vo.setDisplayId(cp.getDisplayId());
            vo.setProblemId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDifficulty(p.getDifficulty());
            result.add(vo);
        }
        return result;
    }

    // ---------- 业务 ----------

    @Override
    @Transactional
    public Long create(ContestCreateRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        Contest contest = new Contest();
        contest.setTitle(request.getTitle());
        contest.setDescription(request.getDescription());
        contest.setType(request.getType());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        if (StringUtils.hasText(request.getPassword())) {
            contest.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        contest.setCreatorId(UserContext.getUserId());
        save(contest);

        if (request.getProblemIds() != null && !request.getProblemIds().isEmpty()) {
            saveProblems(contest.getId(), request.getProblemIds());
        }
        return contest.getId();
    }

    /** 校验题目存在后按顺序写入关联表(题号自动生成 A/B/C...) */
    private void saveProblems(Long contestId, List<Long> problemIds) {
        List<Long> distinct = problemIds.stream().distinct().toList();
        if (distinct.size() != problemIds.size()) {
            throw new IllegalArgumentException("题目列表存在重复");
        }
        if (problemMapper.selectBatchIds(distinct).size() != distinct.size()) {
            throw new IllegalArgumentException("题目列表包含不存在的题目");
        }
        List<ContestProblem> rows = new ArrayList<>();
        for (int i = 0; i < distinct.size(); i++) {
            ContestProblem cp = new ContestProblem();
            cp.setContestId(contestId);
            cp.setProblemId(distinct.get(i));
            cp.setDisplayId(toLetters(i));
            cp.setSort(i);
            rows.add(cp);
        }
        rows.forEach(contestProblemMapper::insert);
    }

    @Override
    public Page<ContestListVO> page(long pageNum, long pageSize) {
        Page<Contest> page = lambdaQuery()
                .orderByDesc(Contest::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        List<Long> creatorIds = page.getRecords().stream()
                .map(Contest::getCreatorId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = creatorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(creatorIds).stream()
                        .collect(Collectors.toMap(User::getId, NicknameUtil::nicknameOf));

        Page<ContestListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(c -> {
            ContestListVO vo = new ContestListVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setType(c.getType());
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
            vo.setHasPassword(StringUtils.hasText(c.getPassword()));
            vo.setStatus(statusOf(c));
            vo.setCreatorName(nameMap.get(c.getCreatorId()));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ContestDetailVO detail(Long id, String contestToken) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        ContestDetailVO vo = new ContestDetailVO();
        vo.setId(contest.getId());
        vo.setTitle(contest.getTitle());
        vo.setDescription(contest.getDescription());
        vo.setType(contest.getType());
        vo.setStartTime(contest.getStartTime());
        vo.setEndTime(contest.getEndTime());
        vo.setHasPassword(StringUtils.hasText(contest.getPassword()));
        vo.setStatus(statusOf(contest));
        User creator = userMapper.selectById(contest.getCreatorId());
        vo.setCreatorName(creator == null ? "未知用户" : NicknameUtil.nicknameOf(creator));
        vo.setCanManage(canManage(contest));

        boolean tokenValid = false;
        if (StringUtils.hasText(contestToken)) {
            try {
                tokenValid = contest.getId().equals(jwtUtil.parseContestToken(contestToken));
            } catch (Exception ignored) {
            }
        }
        vo.setCanEnter(vo.getCanManage() || !vo.getHasPassword() || tokenValid);

        // 题目列表: 已开赛或创建者/管理端可见
        if (vo.getCanManage() || !"NOT_STARTED".equals(vo.getStatus())) {
            List<ContestProblemVO> problems = buildProblemList(contest.getId());
            vo.setProblems(problems);
            vo.setProblemCount(problems.size());
        }
        return vo;
    }

    @Override
    public ContestJoinVO join(Long id, ContestJoinRequest request) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        if (StringUtils.hasText(contest.getPassword()) && !canManage(contest)) {
            if (request == null || !StringUtils.hasText(request.getPassword())
                    || !passwordEncoder.matches(request.getPassword(), contest.getPassword())) {
                throw new IllegalArgumentException("比赛密码错误");
            }
        }
        ContestJoinVO vo = new ContestJoinVO();
        vo.setContestId(contest.getId());
        long ttl = Math.max(Duration.between(LocalDateTime.now(), contest.getEndTime()).toMillis(), 3600_000L)
                + 24 * 3600_000L;
        vo.setToken(jwtUtil.generateContestToken(contest.getId(), UserContext.getUserId(), ttl));
        return vo;
    }

    @Override
    public void update(Long id, ContestUpdateRequest request) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        permissionService.requireAuthorOrManager(contest.getCreatorId());

        boolean started = !LocalDateTime.now().isBefore(contest.getStartTime());
        boolean hasSubmissions = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getContestId, id)) > 0;

        contest.setTitle(request.getTitle());
        contest.setDescription(request.getDescription());
        // 结束时间: 已开赛/已有提交时只可延长
        if ((started || hasSubmissions) && request.getEndTime().isBefore(contest.getEndTime())) {
            throw new IllegalArgumentException("比赛开始后结束时间只能延长");
        }
        contest.setEndTime(request.getEndTime());
        // 开始时间: 仅未开赛且无提交可改
        if (request.getStartTime() != null) {
            if (started || hasSubmissions) {
                throw new IllegalArgumentException("比赛开始后开始时间不可修改");
            }
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new IllegalArgumentException("结束时间必须晚于开始时间");
            }
            contest.setStartTime(request.getStartTime());
        }
        // 密码: null=不改; 空串=取消; 其他=换新
        if (request.getPassword() != null) {
            contest.setPassword(StringUtils.hasText(request.getPassword())
                    ? passwordEncoder.encode(request.getPassword()) : null);
        }
        updateById(contest);
    }

    @Override
    @Transactional
    public void updateProblems(Long id, ContestProblemUpdateRequest request) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        permissionService.requireAuthorOrManager(contest.getCreatorId());
        if (!LocalDateTime.now().isBefore(contest.getStartTime())) {
            throw new IllegalArgumentException("比赛开始后题目不可修改");
        }
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, id));
        saveProblems(id, request.getProblemIds());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        permissionService.requireAuthorOrManager(contest.getCreatorId());
        removeById(id);
        // 比赛提交记录保留(仅删关联)
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, id));
    }

    @Override
    public List<ContestProblemVO> problems(Long id, String contestToken) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        requireContestAccess(contest, contestToken);
        if (!canManage(contest) && LocalDateTime.now().isBefore(contest.getStartTime())) {
            throw new IllegalArgumentException("比赛尚未开始");
        }
        return buildProblemList(id);
    }

    @Override
    public StandingsVO standings(Long id, String contestToken) {
        Contest contest = getById(id);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + id);
        }
        requireContestAccess(contest, contestToken);
        if (!canManage(contest) && LocalDateTime.now().isBefore(contest.getStartTime())) {
            throw new IllegalArgumentException("比赛尚未开始");
        }

        StandingsVO vo = new StandingsVO();
        vo.setType(contest.getType());
        vo.setProblems(buildProblemList(id));

        // OI 赛制: 进行中且非创建者/管理端 → 榜单隐藏
        if (contest.getType() == ContestType.OI && !canManage(contest)
                && LocalDateTime.now().isBefore(contest.getEndTime())) {
            vo.setHidden(true);
            return vo;
        }
        vo.setHidden(false);

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getContestId, id)
                        .ne(Submission::getStatus, JudgeStatus.PENDING)
                        .select(Submission::getUserId, Submission::getProblemId, Submission::getStatus,
                                Submission::getScore, Submission::getCreateTime, Submission::getId));
        // 防脏数据: 拒绝开始时间之前的提交(改过开始时间的残留)
        submissions = submissions.stream()
                .filter(s -> s.getCreateTime() != null && !s.getCreateTime().isBefore(contest.getStartTime()))
                .sorted(Comparator.comparing(Submission::getCreateTime)
                        .thenComparing(Submission::getId))
                .collect(Collectors.toList());

        List<ContestProblemVO> problems = vo.getProblems();
        Map<Long, Integer> problemIndex = new HashMap<>();
        for (int i = 0; i < problems.size(); i++) {
            problemIndex.put(problems.get(i).getProblemId(), i);
        }

        if (contest.getType() == ContestType.ICPC) {
            buildIcpcStandings(vo, submissions, problems, problemIndex, contest);
        } else {
            buildScoreStandings(vo, submissions, problems, problemIndex);
        }

        // 补昵称
        List<Long> userIds = vo.getRows().stream().map(StandingsVO.Row::getUserId).distinct().toList();
        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, NicknameUtil::nicknameOf));
            vo.getRows().forEach(row -> row.setUsername(nameMap.get(row.getUserId())));
        }
        return vo;
    }

    /** ICPC: 解题数 desc → 罚时 asc → 最后 AC 时间 asc; 并列同名次; CE 不计罚时 */
    private void buildIcpcStandings(StandingsVO vo, List<Submission> submissions,
                                    List<ContestProblemVO> problems, Map<Long, Integer> problemIndex,
                                    Contest contest) {
        int n = problems.size();
        // 每用户: 每题是否已 AC / AC 前错误次数 / 首 AC 相对毫秒 / 最后 AC 时间戳(并列决胜)
        Map<Long, boolean[]> solvedMap = new HashMap<>();
        Map<Long, int[]> wrongMap = new HashMap<>();
        Map<Long, long[]> firstAcMap = new HashMap<>();
        Map<Long, Long> lastAcMap = new HashMap<>();

        for (Submission s : submissions) {
            Integer idx = problemIndex.get(s.getProblemId());
            if (idx == null) {
                continue;
            }
            Long uid = s.getUserId();
            boolean[] solved = solvedMap.computeIfAbsent(uid, k -> new boolean[n]);
            if (solved[idx]) {
                continue; // 已 AC 的题后续提交一律忽略
            }
            if (s.getStatus() == JudgeStatus.ACCEPTED) {
                solved[idx] = true;
                long ms = s.getCreateTime() == null ? 0
                        : Duration.between(contest.getStartTime(), s.getCreateTime()).toMillis();
                firstAcMap.computeIfAbsent(uid, k -> new long[n])[idx] = ms;
                long ts = s.getCreateTime() == null ? 0
                        : java.sql.Timestamp.valueOf(s.getCreateTime()).getTime();
                lastAcMap.merge(uid, ts, Math::max);
            } else if (s.getStatus() != JudgeStatus.COMPILE_ERROR) {
                wrongMap.computeIfAbsent(uid, k -> new int[n])[idx]++;
            }
        }

        List<StandingsVO.Row> rows = new ArrayList<>();
        for (Map.Entry<Long, boolean[]> e : solvedMap.entrySet()) {
            Long uid = e.getKey();
            boolean[] solved = e.getValue();
            int[] wrong = wrongMap.get(uid);
            long[] firstAc = firstAcMap.get(uid);
            StandingsVO.Row row = new StandingsVO.Row();
            row.setUserId(uid);
            int solvedCount = 0;
            int penalty = 0;
            List<String> states = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (solved[i]) {
                    solvedCount++;
                    int w = wrong == null ? 0 : wrong[i];
                    penalty += (int) ((firstAc == null ? 0 : firstAc[i]) / 60000) + 20 * w;
                    states.add("+" + w);
                } else if (wrong != null && wrong[i] > 0) {
                    states.add("-" + wrong[i]);
                } else {
                    states.add(null);
                }
            }
            row.setSolved(solvedCount);
            row.setPenalty(penalty);
            row.setProblemStates(states);
            rows.add(row);
        }
        rows.sort(Comparator
                .comparing(StandingsVO.Row::getSolved, Comparator.reverseOrder())
                .thenComparing(StandingsVO.Row::getPenalty)
                .thenComparing(r -> lastAcMap.getOrDefault(r.getUserId(), Long.MAX_VALUE)));
        assignRanks(rows, r -> r.getSolved() + "|" + r.getPenalty());
        vo.setRows(rows);

        List<Long> solvedPerProblem = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int fi = i;
            solvedPerProblem.add(solvedMap.keySet().stream()
                    .filter(uid -> solvedMap.get(uid)[fi]).count());
        }
        vo.setProblemSolvedCount(solvedPerProblem);
    }

    /** OI/IOI: 每题取最高分求和, 总分 desc, 并列同名次 */
    private void buildScoreStandings(StandingsVO vo, List<Submission> submissions,
                                     List<ContestProblemVO> problems, Map<Long, Integer> problemIndex) {
        int n = problems.size();
        Map<Long, Integer[]> best = new HashMap<>();
        for (Submission s : submissions) {
            Integer idx = problemIndex.get(s.getProblemId());
            if (idx == null || s.getScore() == null) {
                continue;
            }
            Integer[] arr = best.computeIfAbsent(s.getUserId(), k -> new Integer[n]);
            if (arr[idx] == null || s.getScore() > arr[idx]) {
                arr[idx] = s.getScore();
            }
        }
        List<StandingsVO.Row> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer[]> e : best.entrySet()) {
            StandingsVO.Row row = new StandingsVO.Row();
            row.setUserId(e.getKey());
            int total = 0;
            for (Integer score : e.getValue()) {
                if (score != null) {
                    total += score;
                }
            }
            row.setTotalScore(total);
            row.setProblemScores(List.of(e.getValue()));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(StandingsVO.Row::getTotalScore, Comparator.reverseOrder()));
        assignRanks(rows, r -> String.valueOf(r.getTotalScore()));
        vo.setRows(rows);
    }

    @Override
    public void checkSubmitAccess(Long contestId, Long problemId, String contestToken) {
        Contest contest = getById(contestId);
        if (contest == null) {
            throw new IllegalArgumentException("比赛不存在: id=" + contestId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getStartTime())) {
            throw new IllegalArgumentException("比赛尚未开始");
        }
        if (now.isAfter(contest.getEndTime())) {
            throw new IllegalArgumentException("比赛已结束");
        }
        requireContestAccess(contest, contestToken);
        Long count = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId));
        if (count == null || count == 0) {
            throw new IllegalArgumentException("题目不在该比赛中");
        }
    }

    private void assignRanks(List<StandingsVO.Row> rows, java.util.function.Function<StandingsVO.Row, String> keyFn) {
        int rank = 0;
        String lastKey = null;
        for (int i = 0; i < rows.size(); i++) {
            String key = keyFn.apply(rows.get(i));
            if (lastKey == null || !lastKey.equals(key)) {
                rank = i + 1;
                lastKey = key;
            }
            rows.get(i).setRank(rank);
        }
    }
}
