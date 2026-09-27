-- The LLD (design/components/02-persistence.md) always specified `tags[]` and
-- `objectives[]` as native array columns on the owning row, distinct from the
-- explicit `content JSONB` column. The initial implementation instead used JPA's
-- default @ElementCollection mapping, which created separate join tables
-- (course_tag, lesson_objective) for what is really just a small, row-owned list
-- of strings with no independent identity or queryable use of its own.

alter table course add column tags text[] not null default '{}';
alter table lesson add column objectives text[] not null default '{}';

update course c
set tags = coalesce((
    select array_agg(ct.tag)
    from course_tag ct
    where ct.course_id = c.id
), '{}');

update lesson l
set objectives = coalesce((
    select array_agg(lo.objective)
    from lesson_objective lo
    where lo.lesson_id = l.id
), '{}');

drop table course_tag;
drop table lesson_objective;
