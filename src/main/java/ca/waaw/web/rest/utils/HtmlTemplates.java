package ca.waaw.web.rest.utils;

import ca.waaw.dto.appnotifications.MailDto;

public final class HtmlTemplates {

    private static final String commonTemplate = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "\n" +
            "<head>\n" +
            "    <meta http-equiv='content-Type' content='text/html; charset=UTF-8'/>\n" +
            "    <style>\n" +
            "        @import url('https://fonts.googleapis.com/css2?family=Mulish:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap');\n" +
            "\n" +
            "        * {\n" +
            "            box-sizing: border-box;\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "\n" +
            "        body {\n" +
            "            font-family: Mulish, sans-sarif;\n" +
            "            font-size: 14px;\n" +
            "            color: #2d3436;\n" +
            "        }\n" +
            "\n" +
            "        a {\n" +
            "            text-decoration: none;\n" +
            "        }\n" +
            "\n" +
            "        .container {\n" +
            "            padding: 10px\n" +
            "        }\n" +
            "\n" +
            "        .team {\n" +
            "            margin: 45px 0;\n" +
            "            list-style: none;\n" +
            "        }\n" +
            "\n" +
            "        .contact,\n" +
            "        .msg {\n" +
            "            display: flex;\n" +
            "            flex-direction: row;\n" +
            "            justify-content: center;\n" +
            "            background-color: #6c5ce7;\n" +
            "            color: #fff;\n" +
            "        }\n" +
            "\n" +
            "        .item {\n" +
            "            padding: 10px;\n" +
            "            font-size: 20px;\n" +
            "            font-weight: bold;\n" +
            "        }\n" +
            "\n" +
            "        .msg {\n" +
            "            font-size: 18px;\n" +
            "            padding: 10px;\n" +
            "            background: #a29bfe;\n" +
            "            color: #2d3436;\n" +
            "        }\n" +
            "\n" +
            "        p {\n" +
            "            padding: 10px;\n" +
            "            font-size: 0.8 rem;\n" +
            "            text-align: justify;\n" +
            "        }\n" +
            "\n" +
            "        .team li {\n" +
            "            margin-bottom: 35px;\n" +
            "        }\n" +
            "\n" +
            "        .divider {\n" +
            "            padding: 2px;\n" +
            "            border-bottom: rgb(20, 109, 243) solid 2px;\n" +
            "            margin-bottom: 5px;\n" +
            "        }\n" +
            "\n" +
            "        .logo {\n" +
            "            width: 50px;\n" +
            "            margin-bottom: 15px;\n" +
            "        }\n" +
            "\n" +
            "        a:visited {\n" +
            "            color: blue;\n" +
            "        }\n" +
            "\n" +
            "        h1 {\n" +
            "            text-align: center;\n" +
            "            padding: 5px 2px;\n" +
            "            padding-top: 15px;\n" +
            "            font-size: 1rem;\n" +
            "        }\n" +
            "\n" +
            "        .content-box {\n" +
            "            border: #9da2a3 solid 1px;\n" +
            "            border-radius: 5px;\n" +
            "            padding-bottom: 15px;\n" +
            "        }\n" +
            "\n" +
            "        .button {\n" +
            "            min-width: 150px;\n" +
            "            width: fit-content;\n" +
            "            text-align: center;\n" +
            "            padding: 12px 25px;\n" +
            "            font-weight: bold;\n" +
            "            border: 1px solid #2996C3;\n" +
            "            border-radius: 5px;\n" +
            "            width: fit-content;\n" +
            "            margin: auto;\n" +
            "            color: #2996C3;\n" +
            "            cursor: pointer;\n" +
            "        }\n" +
            "\n" +
            "        .top-logo {\n" +
            "            margin: auto;\n" +
            "            padding: 2px;\n" +
            "            cursor: pointer;\n" +
            "        }\n" +
            "\n" +
            "        .social a {\n" +
            "            display: inline-block\n" +
            "        }\n" +
            "\n" +
            "        .social .fa {\n" +
            "            height: 20px;\n" +
            "            width: 20px;\n" +
            "            margin: 4px 5px;\n" +
            "        }\n" +
            "\n" +
            "        table {\n" +
            "            width: 100%%;\n" +
            "            border-spacing: 0;\n" +
            "        }\n" +
            "\n" +
            "        table td {\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "\n" +
            "        .user-details {\n" +
            "            width: fit-content;\n" +
            "            margin: 15px auto;\n" +
            "        }\n" +
            "\n" +
            "        .user-details td {\n" +
            "            padding: 5px;\n" +
            "        }\n" +
            "\n" +
            "        .user-details_title {\n" +
            "            font-weight: bold;\n" +
            "            text-align: right;\n" +
            "            padding-right: 50px;\n" +
            "        }\n" +
            "\n" +
            "\n" +
            "    </style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "<div class='body'>\n" +
            "    <div style='margin:auto; max-width: 600px;'>\n" +
            "        <table style='padding: 10px;'>\n" +
            "            <tr>\n" +
            "                <td>\n" +
            "                    <a class='top-logo' href='%1$s' target='_blank'>\n" +
            "                        <img src='%1$s/images/png/favicon.png' class='logo' alt='WAAW'/>\n" +
            "                    </a>\n" +
            "                </td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>\n" +
            "                    <div class='content-box'>\n" +
            "                        <h1>%2$s</h1>\n" +
            "                        <p>\n" +
            "                            <span>Hello %3$s,</span>\n" +
            "                            <br/><br/>\n" +
            "                            <span>%4$s</span>\n" +
            "                        </p>\n" +
            "                        <div style='min-width: 150px; width: 30%%; margin: auto;'>\n" +
            "                            <a href='%5$s'>\n" +
            "                                <div class='button'>%6$s</div>\n" +
            "                            </a>\n" +
            "                        </div>\n" +
            "                        <br/>\n" +
            "                        <p style='text-align: center;'>\n" +
            "                            You can also continue by pasting this link in your address bar:\n" +
            "                        </p>\n" +
            "                        <div>\n" +
            "                            <p style='text-align: center;'>\n" +
            "                                <a href='%5$s'>%5$s</a>\n" +
            "                            </p>\n" +
            "                            <p style='text-align: center;'>For more info visit:\n" +
            "                                <a href='%1$s' target='_blank'>%1$s</a>\n" +
            "                            </p>\n" +
            "                            <div style='margin: auto; width:90%%; border-bottom: #9da2a3 solid 1px;'></div>\n" +
            "                            <p style='text-align: center;'> Find us on:</p>\n" +
            "                            <table style='margin: auto; width:auto;'>\n" +
            "                                <tr>\n" +
            "                                    <td class='social' style='display:inline-block'>\n" +
            "                                        <a href='%7$s'>\n" +
            "                                            <img class='fa' src='%1$s/images/png/twitter.png'\n" +
            "                                                 alt='twitter'/>\n" +
            "                                        </a>\n" +
            "                                        <a href='%7$s'>\n" +
            "                                            <img class='fa' src='%1$s/images/png/linked-in.png'\n" +
            "                                                 alt='linkedin'/>\n" +
            "                                        </a>\n" +
            "                                        <a href='%7$s'>\n" +
            "                                            <img class='fa' src='%1$s/images/png/facebook.png'\n" +
            "                                                 alt='linkedin'/>\n" +
            "                                        </a>\n" +
            "                                        <a href='%7$s'>\n" +
            "                                            <img class='fa' src='%1$s/images/png/instagram.png'\n" +
            "                                                 alt='instagram'/>\n" +
            "                                        </a>\n" +
            "                                    </td>\n" +
            "                                </tr>\n" +
            "                            </table>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </td>\n" +
            "            </tr>\n" +
            "        </table>\n" +
            "    </div>\n" +
            "</div>\n" +
            "</body>\n" +
            "\n" +
            "</html>";

    public static String getCommonTemplate(MailDto message, String messageText, String action) {
        //1. Website url 2. Title 3. Name 4. Message 5. Action url 6. Button text 7. Social dummy url 8. Action
        return String.format(commonTemplate, message.getWebsiteUrl(), message.getTitle(), message.getName(),
                messageText, message.getActionUrl(), message.getButtonText(), "#", action);
    }

}
