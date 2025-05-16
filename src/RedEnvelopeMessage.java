import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.RedEnvelopeMessageInterface;
import com.oocourse.spec3.main.TagInterface;

public class RedEnvelopeMessage implements RedEnvelopeMessageInterface {
    private int id;
    private int socialValue;
    private int type;
    private PersonInterface person1;
    private PersonInterface person2;
    private TagInterface tag;
    private int money;

    public RedEnvelopeMessage(int messageId, int luckyMoney, PersonInterface messagePerson1,
        PersonInterface messagePerson2) {
        this.id = messageId;
        this.socialValue = luckyMoney * 5;
        this.type = 0;
        this.person1 = messagePerson1;
        this.person2 = messagePerson2;
        this.tag = null;
        this.money = luckyMoney;
    }

    public RedEnvelopeMessage(int messageId, int luckyMoney, PersonInterface messagePerson1,
        TagInterface messageTag) {
        this.id = messageId;
        this.socialValue = luckyMoney * 5;
        this.type = 1;
        this.person1 = messagePerson1;
        this.person2 = null;
        this.tag = messageTag;
        this.money = luckyMoney;
    }

    @Override
    public int getMoney() {
        return money;
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