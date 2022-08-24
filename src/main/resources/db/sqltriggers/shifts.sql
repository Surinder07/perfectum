--Trigger to update the last_modified_date on each update
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