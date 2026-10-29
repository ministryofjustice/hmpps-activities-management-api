-- Prisons newly added to ACTIVITIES_LIVE in PR #2306. Preserve configured days.
with prison_codes(prison_code) as (
  values
    ('AGI'),
    ('AYI'),
    ('BAI'),
    ('BFI'),
    ('BLI'),
    ('BNI'),
    ('BRI'),
    ('BSI'),
    ('BWI'),
    ('BXI'),
    ('CDI'),
    ('CFI'),
    ('CKI'),
    ('CLI'),
    ('CWI'),
    ('DHI'),
    ('DTI'),
    ('DWI'),
    ('EHI'),
    ('ESI'),
    ('EWI'),
    ('EXI'),
    ('FDI'),
    ('FHI'),
    ('FYI'),
    ('GHI'),
    ('GMI'),
    ('GNI'),
    ('HBI'),
    ('HCI'),
    ('HDI'),
    ('HHI'),
    ('HLI'),
    ('HMI'),
    ('HOI'),
    ('HPI'),
    ('HVI'),
    ('ISI'),
    ('KMI'),
    ('KVI'),
    ('LCI'),
    ('LFI'),
    ('LHI'),
    ('LII'),
    ('LLI'),
    ('LNI'),
    ('LWI'),
    ('LYI'),
    ('MHI'),
    ('MRI'),
    ('MSI'),
    ('NHI'),
    ('NMI'),
    ('NWI'),
    ('ONI'),
    ('PDI'),
    ('PNI'),
    ('PVI'),
    ('SDI'),
    ('SFI'),
    ('SHI'),
    ('SKI'),
    ('SNI'),
    ('SPI'),
    ('STI'),
    ('SUI'),
    ('SWI'),
    ('TCI'),
    ('UKI'),
    ('UPI'),
    ('VEI'),
    ('WCI'),
    ('WEI'),
    ('WHI'),
    ('WNI'),
    ('WRI'),
    ('WSI'),
    ('WTI'),
    ('WWI'),
    ('WYI')
), days_of_week(day_of_week) as (
  values
    ('MONDAY'),
    ('TUESDAY'),
    ('WEDNESDAY'),
    ('THURSDAY'),
    ('FRIDAY'),
    ('SATURDAY'),
    ('SUNDAY')
), missing_days as (
  select pc.prison_code, dow.day_of_week
  from prison_codes pc
  cross join days_of_week dow
  where not exists (
    select 1
    from prison_regime pr
    join prison_regime_days_of_week prdow on prdow.prison_regime_id = pr.prison_regime_id
    where pr.prison_code = pc.prison_code
      and prdow.day_of_week = dow.day_of_week
  )
), inserted_regimes as (
  insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
  select distinct prison_code, '09:00:00'::time, '12:00:00'::time,
                  '13:00:00'::time, '16:30:00'::time, '18:00:00'::time, '20:00:00'::time
  from missing_days
  returning prison_regime_id, prison_code
)
insert into prison_regime_days_of_week (prison_regime_id, day_of_week)
select pr.prison_regime_id, md.day_of_week
from inserted_regimes pr
join missing_days md on md.prison_code = pr.prison_code;
