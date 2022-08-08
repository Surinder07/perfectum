/* 
	Trigger to update the last_modified_date on each update 
	Written date: 08-Aug-2022
*/

delimiter ;;

CREATE TRIGGER user_updated_by BEFORE UPDATE ON `user` FOR EACH ROW

BEGIN 
	UPDATE `user` SET NEW.last_modified_date = NOW();
END;;

delimiter ;