

-- Hollesey bay, this is disabling outside-work flag (select for convenience, delete before merging)
select a.prison_code, a.summary, a.outside_work from activity a 
where a.prison_code = 'HBI' and a.activity_id=18376)

UPDATE activity SET outside_work = false
WHERE prison_code = 'HBI' and activity_id=18376;

-- Enable outside work for NSI (select for convenience, delete before merging)
select a.prison_code, a.summary, a.outside_work from activity a 
where (a.prison_code = 'NSI' and a.activity_id = 24097 and a.summary = 'Paid Outworker')
or (a.prison_code = 'NSI' and a.activity_id = 25728 and a.summary = 'Counterbalance Conversion  Cola Training')
or (a.prison_code = 'NSI' and a.activity_id = 23933 and a.summary = 'CSV Driver')
or (a.prison_code = 'NSI' and a.activity_id = 24534 and a.summary = 'Freshlinc External Training')
or (a.prison_code = 'NSI' and a.activity_id = 24811 and a.summary = 'Forklift Novice Course (Cola Training)')
or (a.prison_code = 'NSI' and a.activity_id = 24812 and a.summary = 'Forklift Reach Conversion  Cola Training')
or (a.prison_code = 'NSI' and a.activity_id = 24813 and a.summary = 'Forklift TH Conversion  (Cola Training)')
or (a.prison_code = 'NSI' and a.activity_id = 23924 and a.summary = 'Farm Shop')
or (a.prison_code = 'NSI' and a.activity_id = 23999 and a.summary = 'Skills Bootcamp 360 Excavator')
or (a.prison_code = 'NSI' and a.activity_id = 23991 and a.summary = 'Skills Bootcamp NRSWA')
or (a.prison_code = 'NSI' and a.activity_id = 23929 and a.summary = 'Skills Bootcamp - Forward Tipping')
or (a.prison_code = 'NSI' and a.activity_id = 23921 and a.summary = 'Unpaid Work')
;

UPDATE activity SET outside_work = true
WHERE (prison_code = 'NSI' and activity_id = 24097 and summary = 'Paid Outworker')
or (prison_code = 'NSI' and activity_id = 25728 and summary = 'Counterbalance Conversion  Cola Training')
or (prison_code = 'NSI' and activity_id = 23933 and summary = 'CSV Driver')
or (prison_code = 'NSI' and activity_id = 24534 and summary = 'Freshlinc External Training')
or (prison_code = 'NSI' and activity_id = 24811 and summary = 'Forklift Novice Course (Cola Training)')
or (prison_code = 'NSI' and activity_id = 24812 and summary = 'Forklift Reach Conversion  Cola Training')
or (prison_code = 'NSI' and activity_id = 24813 and summary = 'Forklift TH Conversion  (Cola Training)')
or (prison_code = 'NSI' and activity_id = 23924 and summary = 'Farm Shop')
or (prison_code = 'NSI' and activity_id = 23999 and summary = 'Skills Bootcamp 360 Excavator')
or (prison_code = 'NSI' and activity_id = 23991 and summary = 'Skills Bootcamp NRSWA')
or (prison_code = 'NSI' and activity_id = 23929 and summary = 'Skills Bootcamp - Forward Tipping')
or (prison_code = 'NSI' and activity_id = 23921 and summary = 'Unpaid Work')
;