

-- Disable outside work for HBI.
UPDATE activity
SET outside_work = false
WHERE prison_code = 'HBI'
  AND activity_id = 18376;

-- Enable outside work for NSI.
UPDATE activity
SET outside_work = true
WHERE (prison_code = 'NSI' AND activity_id = 24097 AND summary = 'Paid Outworker')
	OR (prison_code = 'NSI' AND activity_id = 25728 AND summary = 'Counterbalance Conversion  Cola Training')
	OR (prison_code = 'NSI' AND activity_id = 23933 AND summary = 'CSV Driver')
	OR (prison_code = 'NSI' AND activity_id = 24534 AND summary = 'Freshlinc External Training')
	OR (prison_code = 'NSI' AND activity_id = 24811 AND summary = 'Forklift Novice Course (Cola Training)')
	OR (prison_code = 'NSI' AND activity_id = 24812 AND summary = 'Forklift Reach Conversion  Cola Training')
	OR (prison_code = 'NSI' AND activity_id = 24813 AND summary = 'Forklift TH Conversion  (Cola Training)')
	OR (prison_code = 'NSI' AND activity_id = 23924 AND summary = 'Farm Shop')
	OR (prison_code = 'NSI' AND activity_id = 23999 AND summary = 'Skills Bootcamp 360 Excavator')
	OR (prison_code = 'NSI' AND activity_id = 23991 AND summary = 'Skills Bootcamp NRSWA')
	OR (prison_code = 'NSI' AND activity_id = 23929 AND summary = 'Skills Bootcamp - Forward Tipping')
	OR (prison_code = 'NSI' AND activity_id = 23921 AND summary = 'Unpaid Work');

-- Enable outside work for LTI.
UPDATE activity
SET outside_work = true
WHERE (prison_code = 'LTI' AND activity_id = 5896 AND summary = 'EXTERNAL GARDENS*')
	OR (prison_code = 'LTI' AND activity_id = 24072 AND summary = 'EXTERNAL GARDENS AM*')
	OR (prison_code = 'LTI' AND activity_id = 24073 AND summary = 'EXTERNAL GARDENS PM*');

-- No changes are required for DHI because new EAs will be created on go-live day.