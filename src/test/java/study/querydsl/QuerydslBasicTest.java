package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeEach(){
        queryFactory = new JPAQueryFactory(em);
        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        //member1을 찾아라.
        Member findMember = em.createQuery(
                "select  m " +
                        "from Member  m " +
                        "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void startQuerydsl(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10,30))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("resutlfetch")
    public void resultfetch(){
        //given
//        List<Member> fetch = queryFactory
//                .select(member)
//                .from(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .select(member)
//                .from(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .select(member)
//                .from(member)
//                .fetchFirst();
//
//        QueryResults<Member> results = queryFactory
//                .select(member)
//                .from(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> results1 = results.getResults();
//        List<Member> content = results1;

        long total = queryFactory
                .select(member)
                .from(member)
                .fetchCount();

        //when



        //then
    }

    /**
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
     */
    @Test
    @DisplayName("sort")
    public void sort(){
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        //when

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

        //then
    }

    @Test
    @DisplayName("pageing1")
    public void pageing1(){
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        //when

        //then
    }
    @Test
    @DisplayName("paging2")
    public void paging2(){
        //given
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        List<Member> result = results.getResults();


        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
        //when

        //then
    }

    @Test
    @DisplayName("aggregation")
    public void aggregation(){
        //given
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);


        //when

        //then
    }

    @Test
    @DisplayName("group")
    public void group(){
        //given
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //10+20

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //30+40
    }

    @Test
    @DisplayName("join")
    public void join(){
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     *세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    @DisplayName("theta_join")
    public void theta_join(){
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        //when

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @DisplayName("회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회")
    /**
     * JPAL: select m,t from MEmber m left join m.team t on t.name = 'teamA'
     */

    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+ tuple);
        }

    }

    /**
     *세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void join_on_no_relration(){
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        //when

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .where(member.username.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = "+ tuple);
        }

    }

    @Autowired
    EntityManagerFactory emf;
    @Test
    @DisplayName("no fetch")
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }
    @Test
    @DisplayName("use fetch")
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    @DisplayName("서브 쿼리 - 나이가 가장 많은 회원 조회")
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    @Test
    @DisplayName("서브 쿼리 - 나이가 평균보다 큰 회원 조회")
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    @Test
    @DisplayName("서브 쿼리 - In절")
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }

    @Test
    @DisplayName("서브 쿼리 - In절")
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple =" + tuple);
        }

    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+ s);
        }
    }

    @Test
    public void complextCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0~20살")
                        .when(member.age.between(21,30)).then("21살~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+ s);
        }
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+ s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username "+username);
            System.out.println("age "+age);
        }

    }

    @Test
    public void findDtoByJPAL(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m"
                        , MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+ memberDto);
        }
    }

    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+ memberDto);
        }
    }

    @Test
    public void findDtoByFields(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+ memberDto);
        }
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+ memberDto);
        }
    }

    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //칼럼과 dto 명칭이 안맞을 때  처리

                        ExpressionUtils.as(JPAExpressions // ExpressionUtils.as(서브쿼리, alias)
                                .select(memberSub.age.max()) //서브쿼리는 이것밖에 방법이 없다.
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = "+ userDto);
        }
    }
    @Test
    public void findUserDtoByConstructor(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username.as("name"), //칼럼과 dto 명칭이 안맞을 때  처리

                        ExpressionUtils.as(JPAExpressions // ExpressionUtils.as(서브쿼리, alias)
                                .select(memberSub.age.max()) //서브쿼리는 이것밖에 방법이 없다.
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = "+ userDto);
        }
    }

    @Test
    @DisplayName("findDtoByQueryProjection")
    public void findDtoByQueryProjection(){
        //given
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //when
        for (MemberDto memberDto : result) {
            System.out.println("memberDto =" + memberDto);
        }

        //then
    }

    @Test
    @DisplayName("dynamicQuery")
    public void dynamicQuery_booleanBuilder(){
        //given
//        String usernameParam = "member1";
        String usernameParam = null;
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

        //when

        //then
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();

        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    @DisplayName("dynamicQuery_whereParam")
    public void dynamicQuery_whereParam(){
        //given
        String usernameParam = null;
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
        //when

        //then
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return  usernameCond != null? member.username.eq(usernameCond) : null;
    }


    private BooleanExpression ageEq(Integer ageCond) {
        return  ageCond != null? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @DisplayName("bulkUpdate")
    public void bulkUpdate(){
        //given
        long count = queryFactory
                .update(member)
                .set(member.username, "비화원")
                .where(member.age.lt(28))
                .execute();
        //벌크 연산은 영속성 컨텍스트 상태를 무시하고 바로 DB에 쿼리를 날리기 때문에, 영속성 컨텍스트 초기화 해야한다.
        em.flush();
        em.clear();
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = "+ member);
        }

        //then
    }

    @Test
    @DisplayName("bulkAdd")
    public void bulkAdd(){
        //given
        long execute = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        //when

        //then
    }

    @Test
    @DisplayName("bulkDelete")
    public void bulkDelete(){
        //given
        long execute = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        //when

        //then
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+ s);
        }
    }

    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate(
//                        "function('lower', {0})"
//                        , member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = "+ s);
        }
    }



}
