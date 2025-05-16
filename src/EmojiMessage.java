import com.oocourse.spec3.main.EmojiMessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

public class EmojiMessage implements EmojiMessageInterface {
    private int emojiId;
    private int id;
    private int socialValue;
    private int type;
    private PersonInterface person1;
    private PersonInterface person2;
    private TagInterface tag;

    public EmojiMessage(int messageId, int emojiNumber, PersonInterface messagePerson1,
        PersonInterface messagePerson2) {
        this.id = messageId;
        this.socialValue = emojiNumber;
        this.person1 = messagePerson1;
        this.person2 = messagePerson2;
        this.tag = null;
        this.type = 0;
        this.emojiId = emojiNumber;
    }

    public EmojiMessage(int messageId, int emojiNumber, PersonInterface messagePerson1,
        TagInterface messageTag) {
        this.emojiId = emojiNumber;
        this.socialValue = emojiNumber;
        this.person1 = messagePerson1;
        this.tag = messageTag;
        this.type = 1;
        this.person2 = null;
        this.id = messageId;
    }

    @Override
    public int getEmojiId() {
        return emojiId;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getSocialValue() {
        return socialValue;
    }

    @Override
    public PersonInterface getPerson1() {
        return person1;
    }

    @Override
    public PersonInterface getPerson2() {
        return person2;
    }

    @Override
    public TagInterface getTag() {
        return tag;
    }
}