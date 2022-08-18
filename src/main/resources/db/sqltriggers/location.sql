--Trigger to update the last_modified_date on each update
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