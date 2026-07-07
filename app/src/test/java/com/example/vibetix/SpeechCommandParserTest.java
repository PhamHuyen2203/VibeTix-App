package com.example.vibetix;

import static org.junit.Assert.assertEquals;

import com.example.vibetix.Accessibility.AssistantCommand;
import com.example.vibetix.Accessibility.SpeechCommandParser;

import org.junit.Test;

/**
 * Unit test cho SpeechCommandParser — chạy bằng: gradlew testDebugUnitTest
 */
public class SpeechCommandParserTest {

    private final SpeechCommandParser parser = new SpeechCommandParser();

    private AssistantCommand.Type typeOf(String text) {
        return parser.parse(text).getType();
    }

    @Test
    public void testSearchCommand() {
        AssistantCommand cmd = parser.parse("tìm sự kiện nhạc rock");
        assertEquals(AssistantCommand.Type.SEARCH_EVENT, cmd.getType());
        assertEquals("nhạc rock", cmd.getArgument());

        cmd = parser.parse("Tìm kiếm sự kiện ca nhạc ở Hà Nội");
        assertEquals(AssistantCommand.Type.SEARCH_EVENT, cmd.getType());
        assertEquals("ca nhạc ở Hà Nội", cmd.getArgument());

        // Không dấu vẫn hiểu được
        cmd = parser.parse("tim su kien hoi cho");
        assertEquals(AssistantCommand.Type.SEARCH_EVENT, cmd.getType());
    }

    @Test
    public void testOpenCommand() {
        AssistantCommand cmd = parser.parse("mở số 1");
        assertEquals(AssistantCommand.Type.OPEN_EVENT, cmd.getType());
        assertEquals(1, SpeechCommandParser.parseIndex(cmd.getArgument()));

        cmd = parser.parse("mở sự kiện số hai");
        assertEquals(AssistantCommand.Type.OPEN_EVENT, cmd.getType());
        assertEquals(2, SpeechCommandParser.parseIndex(cmd.getArgument()));

        cmd = parser.parse("chọn số ba");
        assertEquals(AssistantCommand.Type.OPEN_EVENT, cmd.getType());
        assertEquals(3, SpeechCommandParser.parseIndex(cmd.getArgument()));
    }

    @Test
    public void testSimpleCommands() {
        assertEquals(AssistantCommand.Type.READ_DETAIL,     typeOf("đọc chi tiết"));
        assertEquals(AssistantCommand.Type.READ_DETAIL,     typeOf("đọc thông tin sự kiện"));
        assertEquals(AssistantCommand.Type.DESCRIBE_POSTER, typeOf("mô tả poster"));
        assertEquals(AssistantCommand.Type.DESCRIBE_POSTER, typeOf("mô tả ảnh sự kiện"));
        assertEquals(AssistantCommand.Type.BOOK_TICKET,     typeOf("đặt vé"));
        assertEquals(AssistantCommand.Type.BOOK_TICKET,     typeOf("tôi muốn mua vé"));
        assertEquals(AssistantCommand.Type.GO_BACK,         typeOf("quay lại"));
        assertEquals(AssistantCommand.Type.GO_HOME,         typeOf("về trang chủ"));
        assertEquals(AssistantCommand.Type.HELP,            typeOf("trợ giúp"));
        assertEquals(AssistantCommand.Type.STOP,            typeOf("dừng lại"));
        assertEquals(AssistantCommand.Type.STOP,            typeOf("im lặng"));
        assertEquals(AssistantCommand.Type.REPEAT,          typeOf("nhắc lại"));
    }

    @Test
    public void testNavigationCommands() {
        assertEquals(AssistantCommand.Type.OPEN_MY_TICKETS, typeOf("mở vé của tôi"));
        assertEquals(AssistantCommand.Type.OPEN_MY_TICKETS, typeOf("xem vé"));
        assertEquals(AssistantCommand.Type.OPEN_MY_TICKETS, typeOf("vé của tôi"));
        assertEquals(AssistantCommand.Type.OPEN_EVENTS_TAB, typeOf("mở trang sự kiện"));
        assertEquals(AssistantCommand.Type.OPEN_EVENTS_TAB, typeOf("danh sách sự kiện"));
        assertEquals(AssistantCommand.Type.OPEN_PROFILE,    typeOf("mở trang cá nhân"));
        assertEquals(AssistantCommand.Type.OPEN_PROFILE,    typeOf("hồ sơ của tôi"));
        assertEquals(AssistantCommand.Type.DESCRIBE_SCREEN, typeOf("màn hình này có gì"));
        assertEquals(AssistantCommand.Type.DESCRIBE_SCREEN, typeOf("mô tả màn hình"));
        assertEquals(AssistantCommand.Type.DESCRIBE_SCREEN, typeOf("tôi đang ở đâu"));

        // Không được nhầm với lệnh cũ
        assertEquals(AssistantCommand.Type.BOOK_TICKET, typeOf("đặt vé"));
        assertEquals(AssistantCommand.Type.OPEN_EVENT,  typeOf("mở sự kiện số 1"));
        assertEquals(AssistantCommand.Type.SEARCH_EVENT, typeOf("tìm sự kiện nhạc rock"));
    }

    @Test
    public void testUnknown() {
        assertEquals(AssistantCommand.Type.UNKNOWN, typeOf("xin chào bạn khỏe không"));
        assertEquals(AssistantCommand.Type.UNKNOWN, typeOf(""));
        assertEquals(AssistantCommand.Type.UNKNOWN, parser.parse(null).getType());
    }

    @Test
    public void testParseIndexNumberWords() {
        assertEquals(1,  SpeechCommandParser.parseIndex("một"));
        assertEquals(2,  SpeechCommandParser.parseIndex("hai"));
        assertEquals(4,  SpeechCommandParser.parseIndex("bốn"));
        assertEquals(5,  SpeechCommandParser.parseIndex("số năm"));
        assertEquals(7,  SpeechCommandParser.parseIndex("7"));
        assertEquals(-1, SpeechCommandParser.parseIndex("nhạc rock"));
    }

    @Test
    public void testNormalize() {
        assertEquals("tim su kien nhac o ha noi",
                SpeechCommandParser.normalize("Tìm Sự Kiện Nhạc ở Hà Nội"));
        assertEquals("dat ve", SpeechCommandParser.normalize("Đặt Vé"));
    }
}
