import com.oocourse.spec3.main.ForwardMessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

public class ForwardMessage implements ForwardMessageInterface {
    private int id;
    private int socialValue;
    private int type;
    private PersonInterface person1;
    private PersonInterface person2;
    private TagInterface tag;
    private int articleId;

    public ForwardMessage(int messageId, int article, PersonInterface messagePerson1,
        PersonInterface messagePerson2) {
        this.id = messageId;
        this.articleId = article;
        this.person1 = messagePerson1;
        this.person2 = messagePerson2;
        this.type = 0;
        this.tag = null;
        if (articleId < 0) {
            this.socialValue = (- articleId) % 200;
        }
        else {
            this.socialValue = articleId % 200;
        }
    }

    public ForwardMessage(int messageId, int article, PersonInterface messagePerson1,
        TagInterface messageTag) {
        this.id = messageId;
        this.articleId = article;
        this.person1 = messagePerson1;
        this.person2 = null;
        this.type = 1;
        this.tag = messageTag;
        if (articleId < 0) {
            this.socialValue = (- articleId) % 200;
        }
        else {
            this.socialValue = articleId % 200;
        }
    }

    @Override
    public int getArticleId() {
        return articleId;
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