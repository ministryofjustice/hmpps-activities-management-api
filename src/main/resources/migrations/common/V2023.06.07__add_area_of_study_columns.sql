ALTER TABLE activity_minimum_education_level ADD COLUMN study_area_code varchar(10);
ALTER TABLE activity_minimum_education_level ADD COLUMN study_area_description varchar(60);
DROP INDEX idx_activity_minimum_edu_level_activity_edu_code;
CREATE UNIQUE INDEX idx_activity_minimum_edu_level_activity_edu_code ON activity_minimum_education_level (activity_id, study_area_code, education_level_code);
UPDATE activity_minimum_education_level SET study_area_code = 'ENGLA';
UPDATE activity_minimum_education_level SET study_area_description = 'English Language';
ALTER TABLE activity_minimum_education_level ADD COLUMN study_area_code varchar(10) NOT NULL;
ALTER TABLE activity_minimum_education_level ADD COLUMN study_area_description varchar(60) NOT NULL;