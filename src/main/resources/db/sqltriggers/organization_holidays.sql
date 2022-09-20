--Trigger to update the last_modified_date on each update
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