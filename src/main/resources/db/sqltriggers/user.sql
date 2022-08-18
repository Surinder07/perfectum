--Trigger to update the last_modified_date on each update
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

--Trigger to generate a custom id of format USR-00-00000
--Written date: 11-Aug-2022

DROP TRIGGER IF EXISTS user_custom_id;

//

CREATE TRIGGER user_custom_id BEFORE
    INSERT ON user
    FOR EACH ROW

BEGIN

	DECLARE newCustomIdSeq varchar(500);
    DECLARE newCustomIdSeq5d varchar(500);
    DECLARE newCustomIdSeq2d varchar(500);
    DECLARE prefix varchar(500);
    DECLARE singlePneumonic varchar(500);

    SET @singlePneumonic:= 'USR-';
	SET @newCustomIdSeq:= (select waaw_custom_id from user WHERE waaw_custom_id is not null order by created_date desc limit 1);
    SET @newCustomIdSeq5d:= if ( @newCustomIdSeq is null, '0',(SELECT SUBSTRING_INDEX(@newCustomIdSeq, '-', -1)));
    SET @newCustomIdSeq2d:= if ( @newCustomIdSeq is null, LPAD(0, 2,0),( SUBSTRING_index(replace( @newCustomIdSeq , @singlePneumonic, ''),'-',1) ) );
    SET @newCustomIdSeq5d:= LPAD(@newCustomIdSeq5d, 5, '') + 0;
    IF (@newCustomIdSeq5d = 99999) then
		SET @newCustomIdSeq2d:= LPAD(@newCustomIdSeq2d + 1, 2,0);
        SET @newCustomIdSeq5d:= 0;
    END IF;
    SET @prefix:= concat( @singlePneumonic, @newCustomIdSeq2d, '-' );
    SET NEW.waaw_custom_id = concat(@prefix, LPAD(@newCustomIdSeq5d + 1, 5,0));

END

//