--Triggers to update last_modified_date on different tales

--Trigger to update the last_modified_date on table user
--Written date: 08-Aug-2022
DROP TRIGGER IF EXISTS user_updated_by;
//
CREATE TRIGGER user_updated_by
    BEFORE UPDATE ON `user`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END
//

--Trigger to update the last_modified_date on table organization
--Written date: 08-Aug-2022
DROP TRIGGER IF EXISTS organization_updated_by;
//
CREATE TRIGGER organization_updated_by
    BEFORE UPDATE ON `organization`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END
//

--Trigger to update the last_modified_date on table location
--Written date: 16-Aug-2022
DROP TRIGGER IF EXISTS location_updated_by;
//
CREATE TRIGGER location_updated_by
    BEFORE UPDATE ON `location`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END;
//

--Trigger to update the last_modified_date on table location_role
--Written date: 16-Aug-2022
DROP TRIGGER IF EXISTS location_role_updated_by;
//
CREATE TRIGGER location_role_updated_by
    BEFORE UPDATE ON `location_role`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END
//

--Trigger to update the last_modified_date on table shifts
--Written date: 24-Aug-2022
DROP TRIGGER IF EXISTS shifts_updated_by;
//
CREATE TRIGGER shifts_updated_by
    BEFORE UPDATE ON `shifts`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END;
//

--Trigger to update the last_modified_date on table organization_holidays
--Written date: 19-sep-2022
DROP TRIGGER IF EXISTS organization_holidays_updated_by;
//
CREATE TRIGGER organization_holidays_updated_by
    BEFORE UPDATE ON `organization_holidays`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END;
//

--Trigger to update the last_modified_date on table time_sheets
--Written date: 21-oct-2022
DROP TRIGGER IF EXISTS time_sheets_updated_by;
//
CREATE TRIGGER time_sheets_updated_by
    BEFORE UPDATE ON `time_sheets`
    FOR EACH ROW
BEGIN
	SET NEW.last_modified_date = CURRENT_TIMESTAMP();
END;
//