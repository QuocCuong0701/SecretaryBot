package com.quoccuong.secretarybot.service;

import com.quoccuong.secretarybot.constant.CommonConstant;
import com.quoccuong.secretarybot.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.List;

import static com.quoccuong.secretarybot.utils.CommonUtils.isAdmin;
import static com.quoccuong.secretarybot.utils.CommonUtils.isCuongUsername;

@Service
@RequiredArgsConstructor
public class BotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    private static final String COMMAND_MUTE = "/mute";
    private static final String COMMAND_UNMUTE = "/unmute";
    private static final String COMMAND_HELP = "/help";

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.admin.chat-id}")
    private Long adminChatId;

    private final String[] messageRandom = {
            "Bạn không thể cấm anh tôi ~(￣▽￣)~",
            "Hên xui là tui cấm bạn đó.",
            "Đừng làm điều dại dột.",
            "À thế à.",
            "Cho bạn 5s suy nghĩ lại.",
            "No no no. Bạn không thể làm vậy.",
            "Rẩt nghịch.",
            "Tui là tui ghim đó nha.",
    };

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private void sendMessage(SendMessage sendMessage, User from, String content) {
        TelegramClient telegramClient = new OkHttpTelegramClient(botToken);
        try {
            telegramClient.execute(sendMessage);
            log.info(CommonUtils.toJson(sendMessage));
        } catch (Exception e) {
            String text = String.format("%d - %s - %s. Content: %s%n%s", from.getId(), from.getUserName(), from.getFirstName(), content, e.getMessage());
            SendMessage errorMessage = SendMessage.builder().chatId(adminChatId).text(text).build();
            log.error(CommonUtils.toJson(errorMessage));
            sendMessage(errorMessage, from, content);
        }
    }

    private TelegramClient getTelegramClient() {
        return new OkHttpTelegramClient(botToken);
    }

    private void sendMessage(SendMessage sendMessage) {
        try {
            getTelegramClient().execute(sendMessage);
            log.info(CommonUtils.toJson(sendMessage));
        } catch (Exception e) {
            SendMessage errorMessage = SendMessage.builder().chatId(adminChatId).text(e.getMessage()).build();
            log.error(CommonUtils.toJson(errorMessage));
            sendMessage(errorMessage);
        }
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(message).build();
        sendMessage(sendMessage);
    }

    private void sendMessageParseMode(Long chatId, String message) {
        SendMessage errorMessage = SendMessage.builder().chatId(chatId).text(message).parseMode("MarkdownV2").build();
        sendMessage(errorMessage);
    }

    private void sendMessageTextMention(Long chatId, String message, MessageEntity entity) {
        MessageEntity mentionEntity = MessageEntity.builder()
                .type(CommonConstant.TEXT_MENTION)
                .offset(message.indexOf(entity.getText())) // position where text starts (0-based)
                .length(entity.getText().length()) // length of the name you want clickable
                .user(entity.getUser())
                .build();
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .entities(List.of(mentionEntity))
                .build();
        sendMessage(sendMessage);
    }

    private void reply(String text, Long chatId, Integer replyToMsgId) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("MarkdownV2")
                    .replyToMessageId(replyToMsgId)
                    .build();
            sendMessage(sendMessage);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        User from = message.getFrom();
        Chat chat = message.getChat();
        if ((!chat.isGroupChat() && !chat.isSuperGroupChat()) || !message.hasText()) {
            return;
        }

        String text = message.getText().trim();
        if (text.startsWith("/")) {
            text = text.replace("@" + botUsername, "");
            if (text.startsWith(COMMAND_MUTE)) {
                if (!hasMutePermission(chat.getId(), from, message.getText()) || !isAdminInGroup(chat.getId())) {
                    return;
                }
                doAction(from.getUserName(), text, chat.getId(), message.getEntities(), true);
            } else if (text.startsWith(COMMAND_UNMUTE)) {
                if (!hasMutePermission(chat.getId(), from, message.getText()) || !isAdminInGroup(chat.getId())) {
                    return;
                }
                doAction(null, text, chat.getId(), message.getEntities(), false);
            } else if (text.equals(COMMAND_HELP)) {
                String helpMessage = "Cú pháp:" +
                        "\n\\- " + COMMAND_MUTE + " @taylor\\_swift 5 \\- Cấm chat Taylor Swift trong 5 phút" +
                        "\n\\- " + COMMAND_UNMUTE + " @taylor\\_swift \\- Mở chat Taylor Swift";
                sendMessageParseMode(chat.getId(), helpMessage);
            } else {
                sendMessage(chat.getId(), "Gõ " + COMMAND_HELP + " để thêm chi tiết.");
            }
        }
    }

    /**
     * Check quyền thực hiện action
     * Là Cường mới được thực hiện :))
     */
    private boolean hasMutePermission(Long chatId, User from, String text) {
        boolean hasPermission = StringUtils.hasText(from.getUserName()) && isAdmin(from.getUserName());
        if (!hasPermission) {
            SendMessage sendMessage = SendMessage.builder().chatId(chatId).text("Bạn khum có quyền.\nVui lòng liên hệ Đại K Cường.").build();
            sendMessage(sendMessage, from, text);
        }
        return hasPermission;
    }

    /**
     * Danh sách admin
     */
    private List<ChatMember> getGroupAdmins(String chatId) {
        try {
            GetChatAdministrators getChatAdministrators = new GetChatAdministrators(chatId);
            getChatAdministrators.setChatId(chatId);
            return getTelegramClient().execute(getChatAdministrators);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            sendMessage(adminChatId, e.getMessage());
            sendMessage(Long.parseLong(chatId), "Lỗi rồi check đi @" + CommonConstant.CUONG_USERNAME);
            return List.of();
        }
    }

    private boolean isAdminInGroup(Long chatId) {
        List<ChatMember> admins = getGroupAdmins(chatId.toString());
        User me = getMe();
        boolean isAdmin = admins.stream().anyMatch(e -> me != null && e.getUser().getId().equals(me.getId()));
        if (!isAdmin) {
            sendMessage(chatId, "Mình chưa được cấp quyền bạn ơi (〜￣▽￣)〜");
        }
        return isAdmin;
    }

    public User getMe() {
        try {
            return getTelegramClient().execute(new GetMe());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            sendMessage(adminChatId, e.getMessage());
            return null;
        }
    }

    /**
     * Cấm chat
     */
    private void doAction(String currentUsername, String command, Long chatId, List<MessageEntity> entities, boolean mute) {
        try {
            MessageEntity entity = CollectionUtils.isEmpty(entities) ? null
                    : entities.stream().filter(e -> e.getType().equals(CommonConstant.TEXT_MENTION)).findFirst().orElse(null);
            Long userId = getUserIdFromCommand(currentUsername, chatId, command, entity, mute);
            if (userId != null) {
                String[] args = command.split(" ");
                try {
                    ChatPermissions chatPermissions = getChatPermissions(!mute);
                    double time = 0;
                    if (entity == null) {
                        time = mute ? Double.parseDouble(args[2]) : 0;
                    } else {
                        try {
                            time = Double.parseDouble(command.replace((mute ? COMMAND_MUTE : COMMAND_UNMUTE) + " " + entity.getText(), "").trim());
                        } catch (Exception ignored) {
                        }
                    }
                    RestrictChatMember restrictChatMember = RestrictChatMember.builder()
                            .chatId(chatId)
                            .userId(userId)
                            .permissions(chatPermissions)
                            .build();
                    if (mute) {
                        restrictChatMember.setUntilDate((int) (Instant.now().getEpochSecond() + (time * 60)));
                    }
                    getTelegramClient().execute(restrictChatMember);
                    if (entity == null) {
                        String message = mute
                                ? args[1] + " bạn đã bị cấm chat trong " + getMuteTimeText(time) + "."
                                : args[1] + " bạn đã được ân xá.";
                        sendMessage(chatId, message);
                    } else {
                        String message = mute
                                ? entity.getText() + " bạn đã bị cấm chat trong " + getMuteTimeText(time) + "."
                                : entity.getText() + " bạn đã được ân xá.";
                        sendMessageTextMention(chatId, message, entity);
                    }
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                    sendMessage(adminChatId, e.getMessage());
                    sendMessage(chatId, "Lỗi rồi check đi @" + CommonConstant.CUONG_USERNAME);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            sendMessage(adminChatId, e.getMessage());
        }
    }

    private Long getUserIdFromCommand(String currentUsername, Long chatId, String command, MessageEntity entity, boolean mute) {
        if (entity != null) {
            if (mute) {
                String timeTxt = command.replace((COMMAND_MUTE) + " " + entity.getText(), "").trim();
                if (!StringUtils.hasText(timeTxt)) {
                    sendMessage(chatId, "Cú pháp không hợp lệ.\nGõ " + COMMAND_HELP + " để xem chi tiết.");
                    return null;
                }
                double time = Double.parseDouble(timeTxt);
                if (time <= 0) {
                    sendMessage(chatId, "Thời gian phải lớn hơn 0.");
                    return null;
                }
            }
            return entity.getUser().getId();
        } else {
            String[] args = command.split(" ");
            if (mute) {
                if (args.length != 3 || args[1] == null || !args[1].startsWith("@") || args[2] == null || !NumberUtils.isCreatable(args[2])) {
                    sendMessage(chatId, "Cú pháp không hợp lệ.\nGõ " + COMMAND_HELP + " để xem chi tiết.");
                    return null;
                }
                double time = Double.parseDouble(args[2]);
                if (time <= 0) {
                    sendMessage(chatId, "Thời gian phải lớn hơn 0.");
                    return null;
                }
            } else if (args.length != 2 || args[1] == null || !args[1].startsWith("@")) {
                sendMessage(chatId, "Cú pháp không hợp lệ.\nGõ " + COMMAND_HELP + " để xem chi tiết.");
                return null;
            }
            String username = args[1].substring(1);
            if (mute) {
                if (username.equals(botUsername)) {
                    sendMessage(chatId, messageRandom[CommonUtils.randomNumber(1, messageRandom.length)]);
                    return null;
                } else if (isCuongUsername(username)) {
                    sendMessage(chatId, messageRandom[CommonUtils.randomNumber(0, messageRandom.length)]);
                    int randomNumber = CommonUtils.randomNumber(0, 7);
                    if (StringUtils.hasText(currentUsername) && randomNumber == 2) {
                        doAction(null, COMMAND_MUTE + " @" + currentUsername + " 5", chatId, null, true);
                    }
                    return null;
                }
            }
            Long userId = CommonConstant.USER_ID_IN_GROUP_MAP.get(username);
            if (userId == null) {
                String message = "Không tìm thấy userId của " + args[1];
                log.info(message);
                sendMessage(adminChatId, message);
            }
            return userId;
        }
    }

    /**
     * Quyền hạn chat
     */
    private ChatPermissions getChatPermissions(boolean hasPermission) {
        return ChatPermissions.builder()
                .canSendMessages(hasPermission)
                .canSendAudios(hasPermission)
                .canSendDocuments(hasPermission)
                .canSendPhotos(hasPermission)
                .canSendVideos(hasPermission)
                .canSendVideoNotes(hasPermission)
                .canSendVoiceNotes(hasPermission)
                .canSendPolls(hasPermission)
                .canSendOtherMessages(hasPermission)
                .canAddWebPagePreviews(hasPermission)
                .canChangeInfo(hasPermission)
                .canInviteUsers(hasPermission)
                .canPinMessages(hasPermission)
                .build();
    }

    /**
     * Chuyển đổi thời gian sang text
     */
    private String getMuteTimeText(double time) {
        if (time < 1) {
            return (int) (time * 60) + " giây";
        } else if (time < 60) {
            return (int) time + " phút";
        } else {
            int hour = (int) time / 60;
            if (hour >= 24) {
                int day = hour / 24;
                int subHour = hour - (day * 24);
                int minute = (int) time - (subHour * 60) - (day * 24 * 60);
                return day + " ngày " + subHour + " giờ " + minute + " phút";
            } else {
                int minute = (int) time - (hour * 60);
                return hour + " giờ " + minute + " phút";
            }
        }
    }
}
