package com.quoccuong.secretarybot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quoccuong.secretarybot.constant.CommonConstant;

import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;

public class CommonUtils {

    private CommonUtils() {
    }

    private static final Random random = new Random();

    public static boolean isCuongUsername(String username) {
        return Objects.equals(username, CommonConstant.CUONG_USERNAME);
    }

    public static boolean isAdmin(String username) {
        return CommonConstant.ADMINS.contains(username);
    }

    public static Integer randomNumber(int min, int max) {
        return random.nextInt(min, max);
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL); // Ignore null properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignore non exist properties
        return mapper;
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return (T) getObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

}
