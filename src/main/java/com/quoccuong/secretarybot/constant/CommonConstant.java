package com.quoccuong.secretarybot.constant;

import java.util.List;
import java.util.Map;

public class CommonConstant {
    private CommonConstant() {
    }

    public static final String CUONG_USERNAME = "quoccuong_0701";
    //    public static final String CUONG_USERNAME = "cuongpq_0701"; // test
    public static final List<String> ADMINS = List.of("GroupAnonymousBot", "cuongpq_0701", "quoccuong_0701");

    public static final Map<String, Long> USER_ID_IN_GROUP_MAP = Map.of(
            "quoccuong_477", 7730185852L,
            "vietanh15081999", 6611425459L,
            "Thuthao_PM", 6133748060L,
            "trang", 6824917365L,
            "ba VA", 1634759377L
    );

    public static final String BOT_COMMAND = "bot_command";
    public static final String TEXT_MENTION = "text_mention"; // TH chưa có username
    public static final String MENTION = "mention"; // TH đã có username
}
