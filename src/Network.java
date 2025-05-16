import com.oocourse.spec3.exceptions.ArticleIdNotFoundException;
import com.oocourse.spec3.exceptions.ContributePermissionDeniedException;
import com.oocourse.spec3.exceptions.DeleteArticlePermissionDeniedException;
import com.oocourse.spec3.exceptions.DeleteOfficialAccountPermissionDeniedException;
import com.oocourse.spec3.exceptions.EmojiIdNotFoundException;
import com.oocourse.spec3.exceptions.EqualArticleIdException;
import com.oocourse.spec3.exceptions.EqualEmojiIdException;
import com.oocourse.spec3.exceptions.EqualMessageIdException;
import com.oocourse.spec3.exceptions.EqualOfficialAccountIdException;
import com.oocourse.spec3.exceptions.MessageIdNotFoundException;
import com.oocourse.spec3.exceptions.OfficialAccountIdNotFoundException;
import com.oocourse.spec3.exceptions.PathNotFoundException;
import com.oocourse.spec3.exceptions.PersonIdNotFoundException;
import com.oocourse.spec3.exceptions.EqualPersonIdException;
import com.oocourse.spec3.exceptions.TagIdNotFoundException;
import com.oocourse.spec3.exceptions.EqualRelationException;
import com.oocourse.spec3.exceptions.AcquaintanceNotFoundException;
import com.oocourse.spec3.exceptions.EqualTagIdException;
import com.oocourse.spec3.exceptions.RelationNotFoundException;
import com.oocourse.spec3.main.EmojiMessageInterface;
import com.oocourse.spec3.main.ForwardMessageInterface;
import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.NetworkInterface;
import com.oocourse.spec3.main.OfficialAccountInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.RedEnvelopeMessageInterface;
import com.oocourse.spec3.main.TagInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Network implements NetworkInterface {
    private HashMap<Integer, PersonInterface> persons = new HashMap<>();
    private HashMap<Integer, OfficialAccountInterface> accounts = new HashMap<>();
    private HashMap<Integer, PersonInterface> articlesContributors = new HashMap<>();
    private HashSet<TagInterface> tags = new HashSet<>();
    private int triple = 0;
    private HashMap<Integer, Integer> emojiHeat = new HashMap<>();
    private HashMap<Integer, MessageInterface> messages = new HashMap<>();
    private NetworkTooLong tooLong = new NetworkTooLong(this);

    public Network() {

    }

    @Override
    public boolean containsPerson(int id) {
        return persons.containsKey(id);
    }

    @Override
    public PersonInterface getPerson(int id) {
        if (persons.containsKey(id)) {
            return persons.get(id);
        }
        return null;
    }

    @Override
    public void addPerson(PersonInterface person) throws EqualPersonIdException {
        if (!containsPerson(person.getId())) {
            persons.put(person.getId(), person);
        }
        else {
            throw new EqualPersonIdException(person.getId());
        }
    }

    @Override
    public void addRelation(int id1, int id2, int value)
        throws PersonIdNotFoundException, EqualRelationException {
        if (containsPerson(id1) && containsPerson(id2)
            && !getPerson(id1).isLinked(getPerson(id2))) {
            Person person1 = (Person) getPerson(id1);
            Person person2 = (Person) getPerson(id2);
            updateTagsValueSum(person1, person2, value, true);
            person1.addPerson(person2, value);
            person2.addPerson(person1, value);
            for (PersonInterface person : persons.values()) {
                if (person.isLinked(person2) && person.isLinked(person1)
                    && person.getId() != person1.getId() && person.getId() != person2.getId()) {
                    triple++;
                }
            }
        }
        else if (!containsPerson(id1)) {
            throw new PersonIdNotFoundException(id1);
        }
        else if (!containsPerson(id2)) {
            throw new PersonIdNotFoundException(id2);
        }
        else {
            throw new EqualRelationException(id1, id2);
        }
    }

    public void updateTagsValueSum(Person person1, Person person2, int value, Boolean mode) {
        for (TagInterface tag : tags) {
            if (tag.hasPerson(person1) && tag.hasPerson(person2)) {
                if (mode) {
                    ((Tag) tag).addValueSum(2 * value);
                }
                else {
                    ((Tag) tag).addValueSum(- 2 * person1.queryValue(person2));
                }
            }
        }
    }

    @Override
    public void modifyRelation(int id1, int id2, int value)
        throws PersonIdNotFoundException, EqualPersonIdException, RelationNotFoundException {
        if (containsPerson(id1) && containsPerson(id2)
            && getPerson(id1).isLinked(getPerson(id2)) && id1 != id2) {
            Person person1 = (Person) getPerson(id1);
            Person person2 = (Person) getPerson(id2);
            if (getPerson(id1).queryValue(getPerson(id2)) + value > 0) {
                updateTagsValueSum(person1, person2, value, true);
                person1.addPerson(person2, value + person1.queryValue(person2));
                person2.addPerson(person1, value + person2.queryValue(person1));
            }
            else {
                updateTagsValueSum(person1, person2, value, false);
                person1.delPerson(person2);
                person2.delPerson(person1);
                person1.tagDelPerson(person2);
                person2.tagDelPerson(person1);
                for (PersonInterface person : persons.values()) {
                    if (person.isLinked(person2) && person.isLinked(person1)
                        && person.getId() != person1.getId() && person.getId() != person2.getId()) {
                        triple--;
                    }
                }
            }
        }
        else if (!containsPerson(id1)) {
            throw new PersonIdNotFoundException(id1);
        }
        else if (!containsPerson(id2)) {
            throw new PersonIdNotFoundException(id2);
        }
        else if (id1 == id2) {
            throw new EqualPersonIdException(id1);
        }
        else {
            throw new RelationNotFoundException(id1, id2);
        }
    }

    @Override
    public int queryValue(int id1, int id2)
        throws PersonIdNotFoundException, RelationNotFoundException {
        if (containsPerson(id1) && containsPerson(id2) && getPerson(id1).isLinked(getPerson(id2))) {
            Person person1 = (Person) getPerson(id1);
            Person person2 = (Person) getPerson(id2);
            return person1.queryValue(person2);
        }
        else if (!containsPerson(id1)) {
            throw new PersonIdNotFoundException(id1);
        }
        else if (!containsPerson(id2)) {
            throw new PersonIdNotFoundException(id2);
        }
        else {
            throw new RelationNotFoundException(id1, id2);
        }
    }

    @Override
    public boolean isCircle(int id1, int id2) throws PersonIdNotFoundException {
        if (containsPerson(id1) && containsPerson(id2)) {
            return bfs(id1, id2) >= 0;
        }
        else if (!containsPerson(id1)) {
            throw new PersonIdNotFoundException(id1);
        }
        else {
            throw new PersonIdNotFoundException(id2);
        }
    }

    public int bfs(int node, int key) {
        return tooLong.bfs(node, key);
    }

    @Override
    public int queryTripleSum() {
        return triple;
    }

    @Override
    public void addTag(int personId, TagInterface tag)
        throws PersonIdNotFoundException, EqualTagIdException {
        if (containsPerson(personId) && !getPerson(personId).containsTag(tag.getId())) {
            Person person = (Person) getPerson(personId);
            person.addTag(tag);
            tags.add(tag);
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else {
            throw new EqualTagIdException(tag.getId());
        }
    }

    @Override
    public void addPersonToTag(int personId1, int personId2, int tagId)
        throws PersonIdNotFoundException, RelationNotFoundException,
        TagIdNotFoundException, EqualPersonIdException {
        if (containsPerson(personId1) && containsPerson(personId2) && personId1 != personId2
            && getPerson(personId2).isLinked(getPerson(personId1))
            && getPerson(personId2).containsTag(tagId)
            && !getPerson(personId2).getTag(tagId).hasPerson(getPerson(personId1))) {
            if (getPerson(personId2).getTag(tagId).getSize() <= 999) {
                getPerson(personId2).getTag(tagId).addPerson(getPerson(personId1));
            }
        }
        else if (!containsPerson(personId1)) {
            throw new PersonIdNotFoundException(personId1);
        }
        else if (!containsPerson(personId2)) {
            throw new PersonIdNotFoundException(personId2);
        }
        else if (personId1 == personId2) {
            throw new EqualPersonIdException(personId1);
        }
        else if (!getPerson(personId2).isLinked(getPerson(personId1))) {
            throw new RelationNotFoundException(personId2, personId1);
        }
        else if (!getPerson(personId2).containsTag(tagId)) {
            throw new TagIdNotFoundException(tagId);
        }
        else {
            throw new EqualPersonIdException(personId1);
        }
    }

    @Override
    public int queryTagValueSum(int personId, int tagId)
        throws PersonIdNotFoundException, TagIdNotFoundException {
        if (containsPerson(personId) && getPerson(personId).containsTag(tagId)) {
            return getPerson(personId).getTag(tagId).getValueSum();
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else {
            throw new TagIdNotFoundException(tagId);
        }
    }

    @Override
    public int queryTagAgeVar(int personId, int tagId)
        throws PersonIdNotFoundException, TagIdNotFoundException {
        if (containsPerson(personId) && getPerson(personId).containsTag(tagId)) {
            return getPerson(personId).getTag(tagId).getAgeVar();
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else {
            throw new TagIdNotFoundException(tagId);
        }
    }

    @Override
    public void delPersonFromTag(int personId1, int personId2, int tagId)
        throws PersonIdNotFoundException, TagIdNotFoundException {
        if (containsPerson(personId1) && containsPerson(personId2)
            && getPerson(personId2).containsTag(tagId)
            && getPerson(personId2).getTag(tagId).hasPerson(getPerson(personId1))) {
            Person person = (Person) getPerson(personId1);
            getPerson(personId2).getTag(tagId).delPerson(person);
        }
        else if (!containsPerson(personId1)) {
            throw new PersonIdNotFoundException(personId1);
        }
        else if (!containsPerson(personId2)) {
            throw new PersonIdNotFoundException(personId2);
        }
        else if (!getPerson(personId2).containsTag(tagId)) {
            throw new TagIdNotFoundException(tagId);
        }
        else {
            throw new PersonIdNotFoundException(personId1);
        }
    }

    @Override
    public void delTag(int personId, int tagId)
        throws PersonIdNotFoundException, TagIdNotFoundException {
        if (containsPerson(personId) && getPerson(personId).containsTag(tagId)) {
            tags.remove(getPerson(personId).getTag(tagId));
            getPerson(personId).delTag(tagId);
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else {
            throw new TagIdNotFoundException(tagId);
        }
    }

    @Override
    public boolean containsMessage(int id) {
        return messages.containsKey(id);
    }

    @Override
    public void addMessage(MessageInterface message)
        throws EqualMessageIdException, EmojiIdNotFoundException, EqualPersonIdException,
        ArticleIdNotFoundException {
        if (!containsMessage(message.getId()) && !(message instanceof EmojiMessageInterface
            && !containsEmojiId(((EmojiMessageInterface) message).getEmojiId()))
            && !(message.getType() == 0 && message.getPerson1().equals(message.getPerson2()))
            && !(message instanceof ForwardMessage && !message.getPerson1().getReceivedArticles()
            .contains(((ForwardMessageInterface) message).getArticleId()))) {
            messages.put(message.getId(), message);
        }
        else if (containsMessage(message.getId())) {
            throw new EqualMessageIdException(message.getId());
        }
        else if (message instanceof EmojiMessageInterface
            && !containsEmojiId(((EmojiMessageInterface) message).getEmojiId())) {
            throw new EmojiIdNotFoundException(((EmojiMessageInterface) message).getEmojiId());
        }
        else if ((message instanceof ForwardMessageInterface)
            && !containsArticle(((ForwardMessageInterface) message).getArticleId())) {
            throw new ArticleIdNotFoundException(((ForwardMessageInterface) message)
                .getArticleId());
        }
        else if ((message instanceof ForwardMessageInterface)
            && containsArticle(((ForwardMessageInterface) message).getArticleId())
            && !(message.getPerson1().getReceivedArticles().contains(
            ((ForwardMessageInterface) message).getArticleId()))) {
            throw new ArticleIdNotFoundException(((ForwardMessageInterface) message)
                .getArticleId());
        }
        else if (message.getType() == 0 && message.getPerson1().equals(message.getPerson2())) {
            throw new EqualPersonIdException(message.getPerson1().getId());
        }
    }

    @Override
    public MessageInterface getMessage(int id) {
        if (containsMessage(id)) {
            return messages.get(id);
        }
        else {
            return null;
        }
    }

    @Override
    public void sendMessage(int id)
        throws RelationNotFoundException, MessageIdNotFoundException, TagIdNotFoundException {
        if (containsMessage(id) && getMessage(id).getType() == 0 && getMessage(id).getPerson1()
            .isLinked(getMessage(id).getPerson2()) && getMessage(id).getPerson1()
            != getMessage(id).getPerson2()) {
            MessageInterface message = getMessage(id);
            messages.remove(id);
            message.getPerson1().addSocialValue(message.getSocialValue());
            message.getPerson2().addSocialValue(message.getSocialValue());
            if (message instanceof RedEnvelopeMessageInterface) {
                message.getPerson1().addMoney(- ((RedEnvelopeMessageInterface) message).getMoney());
                message.getPerson2().addMoney(((RedEnvelopeMessageInterface) message).getMoney());
            }
            else if (message instanceof ForwardMessageInterface) {
                ((Person) message.getPerson2()).receiveArticle(((ForwardMessageInterface) message)
                    .getArticleId());
            }
            else if (message instanceof EmojiMessageInterface) {
                emojiHeat.put(((EmojiMessageInterface) message).getEmojiId(),
                    emojiHeat.get(((EmojiMessageInterface) message).getEmojiId()) + 1);
            }
            ((Person) message.getPerson2()).receiveMessage(message);
        }
        else if (containsMessage(id) && getMessage(id).getType() == 1
            && getMessage(id).getPerson1().containsTag(getMessage(id).getTag().getId())) {
            MessageInterface message = getMessage(id);
            messages.remove(id);
            message.getPerson1().addSocialValue(message.getSocialValue());
            ((Tag) message.getTag()).addSocialValue(message.getSocialValue());
            if (message instanceof RedEnvelopeMessageInterface && message.getTag().getSize() > 0) {
                int money = ((RedEnvelopeMessageInterface) message).getMoney()
                    / message.getTag().getSize();
                message.getPerson1().addMoney(- money * message.getTag().getSize());
                ((Tag) message.getTag()).addMoney(money);
            }
            else if (message instanceof ForwardMessageInterface && message.getTag().getSize() > 0) {
                ((Tag) message.getTag()).receiveArticle(
                    ((ForwardMessageInterface) message).getArticleId());
            }
            else if (message instanceof EmojiMessageInterface) {
                emojiHeat.put(((EmojiMessageInterface) message).getEmojiId()
                    , emojiHeat.get(((EmojiMessageInterface) message).getEmojiId()) + 1);
            }
            ((Tag) message.getTag()).receiveMessage(message);
        }
        else if (!containsMessage(id)) {
            throw new MessageIdNotFoundException(id);
        }
        else if (getMessage(id).getType() == 0 && !(getMessage(id).getPerson1()
            .isLinked(getMessage(id).getPerson2()))) {
            throw new RelationNotFoundException(getMessage(id).getPerson1().getId()
                , getMessage(id).getPerson2().getId());
        }
        else {
            throw new TagIdNotFoundException(getMessage(id).getTag().getId());
        }
    }

    @Override
    public int querySocialValue(int id) throws PersonIdNotFoundException {
        if (containsPerson(id)) {
            return getPerson(id).getSocialValue();
        }
        else {
            throw new PersonIdNotFoundException(id);
        }
    }

    @Override
    public List<MessageInterface> queryReceivedMessages(int id) throws PersonIdNotFoundException {
        if (containsPerson(id)) {
            return getPerson(id).getReceivedMessages();
        }
        else {
            throw new PersonIdNotFoundException(id);
        }
    }

    @Override
    public boolean containsEmojiId(int id) {
        return emojiHeat.containsKey(id);
    }

    @Override
    public void storeEmojiId(int id) throws EqualEmojiIdException {
        if (!containsEmojiId(id)) {
            emojiHeat.put(id, 0);
        }
        else {
            throw new EqualEmojiIdException(id);
        }
    }

    @Override
    public int queryMoney(int id) throws PersonIdNotFoundException {
        if (containsPerson(id)) {
            return getPerson(id).getMoney();
        }
        else {
            throw new PersonIdNotFoundException(id);
        }
    }

    @Override
    public int queryPopularity(int id) throws EmojiIdNotFoundException {
        if (containsEmojiId(id)) {
            return emojiHeat.get(id);
        }
        else {
            throw new EmojiIdNotFoundException(id);
        }
    }

    @Override
    public int deleteColdEmoji(int limit) {
        ArrayList<Integer> delete = new ArrayList<>();
        for (Integer emojiId : emojiHeat.keySet()) {
            if (emojiHeat.get(emojiId) < limit) {
                delete.add(emojiId);
            }
        }
        for (Integer emojiId : delete) {
            emojiHeat.remove(emojiId);
        }
        ArrayList<MessageInterface> deletes = new ArrayList<>();
        for (MessageInterface message : messages.values()) {
            if (message instanceof EmojiMessageInterface && !emojiHeat
                .containsKey(((EmojiMessageInterface) message).getEmojiId())) {
                deletes.add(message);
            }
        }
        for (MessageInterface message : deletes) {
            messages.remove(message.getId());
        }
        return emojiHeat.size();
    }

    @Override
    public int queryBestAcquaintance(int id)
        throws PersonIdNotFoundException, AcquaintanceNotFoundException {
        if (containsPerson(id) && !((Person) getPerson(id)).isAcquaintanceEmpty()) {
            Person person = (Person) getPerson(id);
            return person.queryBestAcquaintance();
        }
        else if (!containsPerson(id)) {
            throw new PersonIdNotFoundException(id);
        }
        else {
            throw new AcquaintanceNotFoundException(id);
        }
    }

    @Override
    public int queryCoupleSum() {
        int ans = 0;
        for (int id1 : persons.keySet()) {
            Person person1 = (Person) getPerson(id1);
            if (person1.isAcquaintanceEmpty()) {
                continue;
            }
            int id2 = person1.queryBestAcquaintance();
            Person person2 = (Person) getPerson(id2);
            if (id2 != id1 && !person2.isAcquaintanceEmpty()
                && person2.queryBestAcquaintance() == id1) {
                ans++;
            }
        }
        return ans / 2;
    }

    @Override
    public int queryShortestPath(int id1, int id2)
        throws PersonIdNotFoundException, PathNotFoundException {
        if (containsPerson(id1) && id1 == id2) {
            return 0;
        }
        else if (containsPerson(id1) && containsPerson(id2) && id1 != id2) {
            int len = bfs(id1, id2);
            if (len == -1) {
                throw new PathNotFoundException(id1, id2);
            }
            return len;
        }
        else if (!containsPerson(id1)) {
            throw new PersonIdNotFoundException(id1);
        }
        else {
            throw new PersonIdNotFoundException(id2);
        }
    }

    @Override
    public boolean containsAccount(int id) {
        return accounts.containsKey(id);
    }

    @Override
    public void createOfficialAccount(int personId, int accountId, String name)
        throws PersonIdNotFoundException, EqualOfficialAccountIdException {
        if (containsPerson(personId) && !containsAccount(accountId)) {
            if (!containsAccount(accountId) && containsPerson(personId)) {
                OfficialAccount officialAccount = new OfficialAccount(personId, accountId, name);
                officialAccount.addFollower(getPerson(personId));
                accounts.put(accountId, officialAccount);
            }
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else {
            throw new EqualOfficialAccountIdException(accountId);
        }
    }

    @Override
    public void deleteOfficialAccount(int personId, int accountId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        DeleteOfficialAccountPermissionDeniedException {
        if (containsPerson(personId) && containsAccount(accountId)
            && accounts.get(accountId).getOwnerId() == personId) {
            accounts.remove(accountId);
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else if (!containsAccount(accountId)) {
            throw new OfficialAccountIdNotFoundException(accountId);
        }
        else {
            throw new DeleteOfficialAccountPermissionDeniedException(personId, accountId);
        }
    }

    @Override
    public boolean containsArticle(int id) {
        return articlesContributors.containsKey(id);
    }

    @Override
    public void contributeArticle(int personId, int accountId, int articleId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        EqualArticleIdException, ContributePermissionDeniedException {
        if (containsPerson(personId) && containsAccount(accountId) && !containsArticle(articleId)
            && accounts.get(accountId).containsFollower(getPerson(personId))) {
            articlesContributors.put(articleId, getPerson(personId));
            accounts.get(accountId).addArticle(getPerson(personId), articleId);
            ((OfficialAccount) accounts.get(accountId)).dispatchArticle(articleId);
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else if (!containsAccount(accountId)) {
            throw new OfficialAccountIdNotFoundException(accountId);
        }
        else if (containsArticle(articleId)) {
            throw new EqualArticleIdException(articleId);
        }
        else {
            throw new ContributePermissionDeniedException(personId, articleId);
        }
    }

    @Override
    public void deleteArticle(int personId, int accountId, int articleId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        ArticleIdNotFoundException, DeleteArticlePermissionDeniedException {
        if (containsPerson(personId) && containsAccount(accountId) &&
            accounts.get(accountId).containsArticle(articleId) &&
            accounts.get(accountId).getOwnerId() == personId) {
            ((OfficialAccount) accounts.get(accountId)).delArticle(articleId);
        } else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        } else if (!containsAccount(accountId)) {
            throw new OfficialAccountIdNotFoundException(accountId);
        } else if (!accounts.get(accountId).containsArticle(articleId)) {
            throw new ArticleIdNotFoundException(articleId);
        } else {
            throw new DeleteArticlePermissionDeniedException(personId, articleId);
        }
    }

    @Override
    public void followOfficialAccount(int personId, int accountId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        EqualPersonIdException {
        if (containsPerson(personId) && containsAccount(accountId)
            && !accounts.get(accountId).containsFollower(getPerson(personId))) {
            accounts.get(accountId).addFollower(getPerson(personId));
        }
        else if (!containsPerson(personId)) {
            throw new PersonIdNotFoundException(personId);
        }
        else if (!containsAccount(accountId)) {
            throw new OfficialAccountIdNotFoundException(accountId);
        }
        else {
            throw new EqualPersonIdException(personId);
        }
    }

    @Override
    public int queryBestContributor(int id) throws OfficialAccountIdNotFoundException {
        if (containsAccount(id)) {
            return accounts.get(id).getBestContributor();
        }
        else {
            throw new OfficialAccountIdNotFoundException(id);
        }
    }

    @Override
    public List<Integer> queryReceivedArticles(int id) throws PersonIdNotFoundException {
        if (containsPerson(id)) {
            return persons.get(id).queryReceivedArticles();
        }
        else {
            throw new PersonIdNotFoundException(id);
        }
    }

    public MessageInterface[] getMessages() {
        return null;
    }

    public int[] getEmojiIdList() {
        return null;
    }

    public int[] getEmojiHeatList() {
        return null;
    }
}
