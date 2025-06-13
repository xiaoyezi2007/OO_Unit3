import random
import subprocess
import threading
import time
import os
from queue import Queue
import collections  # Import collections
from datetime import datetime

# --- 配置区 ---
# 将这里的 'path/to/your/jarX.jar' 替换成你的 7 个 JAR 文件的实际路径
JAR_FILES = [
    'JAR/hmx.jar',
    'JAR/hzy.jar',
    'JAR/dyc.jar',
    # 'JAR/hmxbianli.jar',
    'JAR/oyzh.jar',
    # 'path/to/your/jar6.jar',
    # 'path/to/your/jar7.jar',
    # 如果少于 7 个，可以注释掉多余的行或只保留你需要的数量
]
NUM_JARS = len(JAR_FILES)
NUM_COMMANDS_PER_TEST = 12000  # 每次对拍生成的指令数量 (可以增加)
MAX_ID = 1000  # 生成的 ID 最大值（可调整）
MIN_ID = -1000  # 生成的 ID 最小值（可调整）
MAX_VALUE_AGE = 200  # value 和 age 的最大值
MIN_M_VAL = -200  # m_val 的最小值
MAX_M_VAL = 200  # m_val 的最大值
MAX_NAME_LEN = 10  # 生成的名字最大长度（可调整）
MAX_ARTICLE_NAME_LEN = 10  # 文章名长度 (increased from 1 for more variability)
LOG_FILE = 'error_log.txt'  # Changed log file name
TIMEOUT_SECONDS = 1.0  # 单个 JAR 执行超时时间 (秒) - 可能需要增加
OUTPUT_DIR = 'test_results'

# --- New Constants for HW3 ---
MAX_SOCIAL_VALUE = 1000
MIN_SOCIAL_VALUE = -1000
MAX_RED_ENVELOPE_MONEY = 200
MAX_EMOJI_ID = 1000  # Assuming emoji IDs are positive and within a reasonable range
MIN_EMOJI_ID = 1  # Assuming emoji IDs are positive
MAX_MESSAGE_ID = 2000  # Separate range for message IDs to avoid collision with other IDs
MIN_MESSAGE_ID = 1000

# --- 状态跟踪 ---
# HW1 State
existing_person_ids = set()
existing_tags = collections.defaultdict(set)  # {person_id: set(tag_ids)}
existing_relations = set()  # set of frozenset({id1, id2})
tag_members = collections.defaultdict(set)  # {(tag_owner_id, tag_id): set(person_id)}

# HW2 State
existing_account_ids = set()
account_owners = {}  # {account_id: owner_person_id}
existing_articles = collections.defaultdict(set)  # {account_id: set(article_id)} (articles belonging to an account)
all_article_ids = set()  # Keep a global set of all article_ids for forwarding
articles_metadata = {}  # {(account_id, article_id): {'owner': person_id, 'name': name}}
account_followers = collections.defaultdict(set)  # {account_id: set(follower_person_id)}
person_followed_accounts = collections.defaultdict(set)  # {person_id: set(account_id)}

# HW3 State
existing_message_ids = set()
network_stored_emoji_ids = set()  # Emoji IDs stored globally in the network via sei
message_details = {}  # {message_id: {'type': 0 or 1, 'msg_type': 'normal'/'emoji'/'red_envelope'/'forward', 'sender': p1, 'receiver_person': p2_or_none, 'receiver_tag': tag_or_none, ...}}


# Person-specific attributes (mainly for query commands, actual logic is in JARs)
# Initial values are 0 as per guide, but we don't need to track them as strictly in generator
# as the JARs handle the logic. These are more for making query commands slightly more meaningful if desired.
# person_money = collections.defaultdict(int)
# person_social_value = collections.defaultdict(int)


def ensure_output_dir():
    """确保输出目录存在"""
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)


def create_test_case_dir(test_count):
    """为每个测试用例创建单独的目录"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    dir_name = f"test_{test_count}"  # Added timestamp for uniqueness
    dir_path = os.path.join(OUTPUT_DIR, dir_name)
    os.makedirs(dir_path, exist_ok=True)
    return dir_path


# --- 数据生成 ---

def generate_random_string(length):
    """生成指定长度的随机字符串 (字母和数字)"""
    chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return ''.join(random.choice(chars) for _ in range(length))


def generate_random_id(prefix="", min_val=MIN_ID, max_val=MAX_ID):
    """生成一个随机 ID, 可选前缀用于区分, and value range"""
    return random.randint(min_val, max_val)


# --- Updated ID Selection ---
def get_id_for_command(id_type='person', use_existing_prob=0.8,
                       target_person_id=None, target_account_id=None,
                       min_val=MIN_ID, max_val=MAX_ID,
                       specific_set_to_use=None):
    """
    Decides whether to return an existing ID or a random (potentially non-existing) ID.
    id_type: 'person', 'tag', 'account', 'article', 'message', 'emoji_stored'.
    use_existing_prob: Probability to try using an existing ID.
    target_person_id: If id_type is 'tag', specify the person whose tags to choose from.
    target_account_id: If id_type is 'article', specify the account whose articles to choose from.
    min_val, max_val: Range for generating new IDs.
    specific_set_to_use: Directly use this set if provided and use_existing is true.
    """
    use_existing = random.random() < use_existing_prob
    chosen_id = None

    if use_existing:
        source_set = None
        if specific_set_to_use is not None:
            source_set = specific_set_to_use
        elif id_type == 'person' and existing_person_ids:
            source_set = existing_person_ids
        elif id_type == 'account' and existing_account_ids:
            source_set = existing_account_ids
        elif id_type == 'message' and existing_message_ids:
            source_set = existing_message_ids
        elif id_type == 'emoji_stored' and network_stored_emoji_ids:  # For qp, dce
            source_set = network_stored_emoji_ids
        elif id_type == 'article':  # Global articles for forwarding, or specific account articles
            possible_articles_list = []
            if target_account_id is not None and target_account_id in existing_articles:
                possible_articles_list = list(existing_articles[target_account_id])
            elif all_article_ids:  # For forwarding, any article might be chosen
                possible_articles_list = list(all_article_ids)
            if possible_articles_list:
                chosen_id = random.choice(possible_articles_list)
        elif id_type == 'tag':
            possible_tags = []
            if target_person_id is not None and target_person_id in existing_tags:
                possible_tags = list(existing_tags[target_person_id])
            elif target_person_id is None:
                for pid in existing_tags:
                    possible_tags.extend(list(existing_tags[pid]))
            if possible_tags:
                chosen_id = random.choice(possible_tags)

        if source_set and chosen_id is None:  # if specific logic above didn't set chosen_id
            if isinstance(source_set, collections.defaultdict):  # Should not happen with current logic
                all_items = [item for sublist in source_set.values() for item in sublist if sublist]
                if all_items: chosen_id = random.choice(all_items)
            elif source_set:  # For sets
                chosen_id = random.choice(list(source_set))

    if chosen_id is None:  # If didn't choose an existing ID (either by probability or because none exist/applicable)
        chosen_id = generate_random_id(min_val=min_val, max_val=max_val)

    return chosen_id


def get_two_ids_for_command(use_existing_prob1=0.8, use_existing_prob2=0.8):
    """Gets two Person IDs, potentially different, respecting existence probability."""
    id1 = get_id_for_command(id_type='person', use_existing_prob=use_existing_prob1)
    id2 = get_id_for_command(id_type='person', use_existing_prob=use_existing_prob2)
    attempts = 0
    while id1 == id2 and attempts < 10:
        id2 = get_id_for_command(id_type='person', use_existing_prob=use_existing_prob2)
        attempts += 1
        if attempts == 10 and id1 == id2:  # Force different if still same
            new_id2 = generate_random_id()
            while new_id2 == id1:
                new_id2 = generate_random_id()
            id2 = new_id2
    return id1, id2


# --- HW1 Command Generators ---

def generate_add_person():
    person_id = generate_random_id()
    attempts = 0
    while person_id in existing_person_ids and attempts < 20:
        person_id = generate_random_id()
        attempts += 1
    if person_id in existing_person_ids:  # If still collision, try to find next available
        candidate = max(existing_person_ids | {MAX_ID}) + 1
        if candidate > MAX_ID: candidate = min(existing_person_ids | {MIN_ID}) - 1  # go other way if maxed out
        person_id = candidate
        if person_id in existing_person_ids: return None  # give up if still collision

    name = generate_random_string(random.randint(1, MAX_NAME_LEN))
    age = random.randint(1, MAX_VALUE_AGE)
    existing_person_ids.add(person_id)
    # person_money[person_id] = 0 # Initialize money
    # person_social_value[person_id] = 0 # Initialize social value
    return f"ap {person_id} {name} {age}"


def generate_add_relation():
    if len(existing_person_ids) < 2: return None  # Need at least 2 people
    id1, id2 = get_two_ids_for_command(use_existing_prob1=0.95, use_existing_prob2=0.95)
    value = random.randint(1, MAX_VALUE_AGE)
    relation = frozenset({id1, id2})
    if id1 in existing_person_ids and id2 in existing_person_ids and id1 != id2:  # and relation not in existing_relations:
        existing_relations.add(relation)
    return f"ar {id1} {id2} {value}"


def generate_modify_relation():
    id1, id2 = None, None
    if existing_relations and random.random() < 0.8:  # Modify existing relation
        relation_tuple = random.sample(list(existing_relations), 1)[0]
        ids = list(relation_tuple)
        id1, id2 = ids[0], ids[1]
    else:  # Try to modify, potentially non-existing
        if len(existing_person_ids) < 2 and not existing_relations: return None
        id1, id2 = get_two_ids_for_command(use_existing_prob1=0.7, use_existing_prob2=0.7)

    m_val = random.randint(MIN_M_VAL, MAX_M_VAL)
    return f"mr {id1} {id2} {m_val}"


def generate_add_tag():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.4,
                                target_person_id=person_id)  # More new tags initially
    if person_id in existing_person_ids:  # Only add if person exists
        existing_tags[person_id].add(tag_id)
    return f"at {person_id} {tag_id}"


def generate_del_tag():
    if not existing_tags: return None
    # Choose a person who has tags
    person_with_tags = [pid for pid, tags in existing_tags.items() if tags]
    if not person_with_tags: return None
    person_id = random.choice(person_with_tags)
    tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.9, target_person_id=person_id)  # Likely existing tag

    if person_id in existing_tags and tag_id in existing_tags[person_id]:
        existing_tags[person_id].remove(tag_id)
        if not existing_tags[person_id]: del existing_tags[person_id]
        tag_key = (person_id, tag_id)
        if tag_key in tag_members: del tag_members[tag_key]  # Also clear members
    return f"dt {person_id} {tag_id}"


def generate_add_to_tag():
    if len(existing_person_ids) < 2 or not existing_tags: return None
    # Select tag owner (person_id2) who has tags and a relation with person_id1
    valid_tag_owners = []
    for p_owner_id, tags_set in existing_tags.items():
        if tags_set:
            valid_tag_owners.append(p_owner_id)
    if not valid_tag_owners: return None
    person_id2 = random.choice(valid_tag_owners)  # Tag owner

    # Select person_id1 who has a relation with person_id2
    potential_members = []
    for p1 in existing_person_ids:
        if p1 != person_id2 and frozenset({p1, person_id2}) in existing_relations:
            potential_members.append(p1)
    if not potential_members:
        person_id1 = get_id_for_command(id_type='person', use_existing_prob=0.7)  # Fallback
    else:
        person_id1 = random.choice(potential_members)

    tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.9,
                                target_person_id=person_id2)  # Existing tag for owner

    # Update state if command is likely valid according to simplified rules
    relation_exists = frozenset({person_id1, person_id2}) in existing_relations
    tag_exists_for_owner = person_id2 in existing_tags and tag_id in existing_tags[person_id2]

    if person_id1 in existing_person_ids and person_id2 in existing_person_ids and \
            person_id1 != person_id2 and relation_exists and tag_exists_for_owner:
        tag_members[(person_id2, tag_id)].add(person_id1)
    return f"att {person_id1} {person_id2} {tag_id}"


def generate_del_from_tag():
    if not tag_members: return None  # Need members in tags to delete
    # Choose a tag that has members
    tag_with_members_keys = [key for key, members in tag_members.items() if members]
    if not tag_with_members_keys: return None
    person_id2, tag_id = random.choice(tag_with_members_keys)  # tag owner and tag_id

    # Choose person_id1 from the members of this tag
    person_id1 = get_id_for_command(id_type='person', use_existing_prob=0.9,
                                    specific_set_to_use=tag_members[(person_id2, tag_id)])

    if (person_id2, tag_id) in tag_members and person_id1 in tag_members[(person_id2, tag_id)]:
        tag_members[(person_id2, tag_id)].remove(person_id1)
        if not tag_members[(person_id2, tag_id)]: del tag_members[(person_id2, tag_id)]
    return f"dft {person_id1} {person_id2} {tag_id}"


def generate_query_value():
    if len(existing_person_ids) < 2: return None
    id1, id2 = get_two_ids_for_command(use_existing_prob1=0.9, use_existing_prob2=0.9)
    return f"qv {id1} {id2}"


def generate_query_circle():
    if len(existing_person_ids) < 2: return None
    id1, id2 = get_two_ids_for_command(use_existing_prob1=0.9, use_existing_prob2=0.9)
    return f"qci {id1} {id2}"


def generate_query_triple_sum():
    return "qts"


def generate_query_tag_age_var():
    if not existing_tags: return None
    person_with_tags = [pid for pid, tags in existing_tags.items() if tags]
    if not person_with_tags: return None  # person must have tag for this query to be meaningful
    person_id = random.choice(person_with_tags)
    tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.9, target_person_id=person_id)
    return f"qtav {person_id} {tag_id}"


def generate_query_best_acquaintance():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.95)
    return f"qba {person_id}"


# --- HW2 Command Generators ---

def generate_create_official_account():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.95)  # Owner should exist
    account_id = generate_random_id()
    attempts = 0
    while account_id in existing_account_ids and attempts < 20:
        account_id = generate_random_id();
        attempts += 1
    if account_id in existing_account_ids: return None  # Avoid too many collisions

    account_name = generate_random_string(random.randint(1, MAX_NAME_LEN))

    if person_id in existing_person_ids and account_id not in existing_account_ids:
        existing_account_ids.add(account_id)
        account_owners[account_id] = person_id
    return f"coa {person_id} {account_id} {account_name}"


def generate_delete_official_account():
    if not existing_account_ids: return None
    account_id = get_id_for_command(id_type='account', use_existing_prob=0.9)  # Account to delete

    # Decide who attempts deletion: owner or other
    person_id = None
    if account_id in account_owners and random.random() < 0.8:  # High chance owner attempts
        person_id = account_owners[account_id]
    else:  # Non-owner or random person attempts
        person_id = get_id_for_command(id_type='person', use_existing_prob=0.7)

    # State update if deletion is valid (owner matches person)
    if account_id in account_owners and account_owners[account_id] == person_id:
        if account_id in existing_account_ids: existing_account_ids.remove(account_id)
        del account_owners[account_id]  # remove from owners
        if account_id in existing_articles: del existing_articles[account_id]
        if account_id in account_followers:
            for follower_id in list(account_followers[account_id]):  # Iterate copy
                if follower_id in person_followed_accounts:
                    person_followed_accounts[follower_id].discard(account_id)
                    if not person_followed_accounts[follower_id]: del person_followed_accounts[follower_id]
            del account_followers[account_id]
        keys_to_delete = [k for k in articles_metadata if k[0] == account_id]
        for k in keys_to_delete:
            if k in articles_metadata: del articles_metadata[k]
            all_article_ids.discard(k[1])  # Remove from global list too

    return f"doa {person_id} {account_id}"


def generate_contribute_article():
    if not existing_person_ids or not existing_account_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    account_id = get_id_for_command(id_type='account', use_existing_prob=0.9)
    article_id = generate_random_id()
    article_name = generate_random_string(random.randint(1, MAX_ARTICLE_NAME_LEN))

    attempts = 0
    while (account_id in existing_articles and article_id in existing_articles[account_id]) and attempts < 10:
        article_id = generate_random_id();
        attempts += 1
    if account_id in existing_articles and article_id in existing_articles[account_id]: return None  # Give up

    article_key = (account_id, article_id)
    if account_id in existing_account_ids:  # and article_key not in articles_metadata: (allow re-contribution by spec?)
        # Guide for HW2 doesn't explicitly forbid re-adding same article ID if deleted.
        # Assuming new article_id must be unique per account at point of adding
        existing_articles[account_id].add(article_id)
        all_article_ids.add(article_id)
        articles_metadata[article_key] = {'owner': person_id, 'name': article_name}

    return f"ca {person_id} {account_id} {article_id} {article_name}"


def generate_delete_article():
    if not all_article_ids: return None  # Need articles to delete
    # Choose an existing article to target
    article_key_to_delete = None
    if articles_metadata and random.random() < 0.9:  # High chance to target existing
        article_key_to_delete = random.choice(list(articles_metadata.keys()))
        account_id, article_id = article_key_to_delete
    else:  # Target potentially non-existing
        account_id = get_id_for_command(id_type='account', use_existing_prob=0.7)
        article_id = get_id_for_command(id_type='article', use_existing_prob=0.5, target_account_id=account_id)
        article_key_to_delete = (account_id, article_id)

    # Decide who attempts deletion
    person_id = None
    if article_key_to_delete in articles_metadata and random.random() < 0.4:  # Article contributor
        person_id = articles_metadata[article_key_to_delete]['owner']
    elif account_id in account_owners and random.random() < 0.7:  # Account owner
        person_id = account_owners[account_id]
    else:  # Random person
        person_id = get_id_for_command(id_type='person', use_existing_prob=0.6)

    # State update logic
    is_account_owner = account_id in account_owners and account_owners[account_id] == person_id
    is_article_contributor = article_key_to_delete in articles_metadata and articles_metadata[article_key_to_delete][
        'owner'] == person_id

    if (is_account_owner or is_article_contributor) and \
            account_id in existing_articles and article_id in existing_articles[account_id]:
        existing_articles[account_id].remove(article_id)
        if not existing_articles[account_id]: del existing_articles[account_id]
        if article_key_to_delete in articles_metadata: del articles_metadata[article_key_to_delete]
        all_article_ids.discard(article_id)

    return f"da {person_id} {account_id} {article_id}"


def generate_follow_official_account():
    if not existing_person_ids or not existing_account_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    account_id = get_id_for_command(id_type='account', use_existing_prob=0.9)

    if person_id in existing_person_ids and account_id in existing_account_ids:
        account_followers[account_id].add(person_id)
        person_followed_accounts[person_id].add(account_id)
    return f"foa {person_id} {account_id}"


def generate_query_shortest_path():
    if len(existing_person_ids) < 2: return None
    id1, id2 = get_two_ids_for_command(use_existing_prob1=0.95, use_existing_prob2=0.95)
    return f"qsp {id1} {id2}"


def generate_query_best_contributor():
    if not existing_account_ids: return None
    account_id = get_id_for_command(id_type='account', use_existing_prob=0.9)
    return f"qbc {account_id}"


def generate_query_received_articles():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    return f"qra {person_id}"


def generate_query_tag_value_sum():
    if not existing_tags: return None
    person_with_tags = [pid for pid, tags in existing_tags.items() if tags]
    if not person_with_tags: return None
    person_id = random.choice(person_with_tags)
    tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.9, target_person_id=person_id)
    return f"qtvs {person_id} {tag_id}"


def generate_query_couple_sum():
    return "qcs"


# --- HW3 Command Generators ---

def generate_add_message_base(msg_type_str, specific_field_val, field_name):
    if not existing_person_ids: return None

    message_id = generate_random_id(min_val=MIN_MESSAGE_ID, max_val=MAX_MESSAGE_ID)
    attempts = 0
    while message_id in existing_message_ids and attempts < 20:
        message_id = generate_random_id(min_val=MIN_MESSAGE_ID, max_val=MAX_MESSAGE_ID);
        attempts += 1
    if message_id in existing_message_ids: return None  # Give up

    msg_delivery_type = random.randint(0, 1)  # 0 for person2, 1 for tag
    person_id1 = get_id_for_command(id_type='person', use_existing_prob=0.9)
    person_id2_or_tag_id = None
    receiver_person_id = None
    receiver_tag_id = None

    if msg_delivery_type == 0:  # Send to person2
        if len(
                existing_person_ids) < 2 and person_id1 not in existing_person_ids: return None  # Need a valid sender and at least one other for receiver
        person_id2_or_tag_id = get_id_for_command(id_type='person', use_existing_prob=0.8)
        attempts = 0
        while person_id2_or_tag_id == person_id1 and attempts < 5 and len(
                existing_person_ids) > 1:  # Ensure different if possible
            person_id2_or_tag_id = get_id_for_command(id_type='person', use_existing_prob=0.8)
            attempts += 1
        if person_id2_or_tag_id == person_id1 and len(existing_person_ids) > 1:  # Force different if still same
            temp_ids = list(existing_person_ids - {person_id1})
            if temp_ids:
                person_id2_or_tag_id = random.choice(temp_ids)
            else:
                return None  # Cannot find a different person
        receiver_person_id = person_id2_or_tag_id
    else:  # Send to tag
        if not existing_tags: return None  # Need tags to send to
        # Try to pick a tag owned by person_id1 or any tag
        owner_for_tag_selection = None
        if random.random() < 0.5 and person_id1 in existing_tags and existing_tags[person_id1]:
            owner_for_tag_selection = person_id1
        person_id2_or_tag_id = get_id_for_command(id_type='tag', use_existing_prob=0.8,
                                                  target_person_id=owner_for_tag_selection)
        receiver_tag_id = person_id2_or_tag_id

    # Common logic for add_message, add_emoji_message etc.
    # The guide states Runner shields invalid person/tag IDs for these commands,
    # so we don't need to be super strict here on existence for p1, p2, tag_id,
    # but using existing ones makes tests more meaningful.

    existing_message_ids.add(message_id)
    details = {
        'type': msg_delivery_type, 'msg_specific_type': msg_type_str,
        'sender': person_id1, 'receiver_person': receiver_person_id, 'receiver_tag': receiver_tag_id
    }
    details[field_name] = specific_field_val
    message_details[message_id] = details

    if msg_type_str == "am":  # add_message
        social_value = specific_field_val
        return f"am {message_id} {social_value} {msg_delivery_type} {person_id1} {person_id2_or_tag_id}"
    elif msg_type_str == "aem":  # add_emoji_message
        emoji_id = specific_field_val
        return f"aem {message_id} {emoji_id} {msg_delivery_type} {person_id1} {person_id2_or_tag_id}"
    elif msg_type_str == "arem":  # add_red_envelope_message
        money = specific_field_val
        return f"arem {message_id} {money} {msg_delivery_type} {person_id1} {person_id2_or_tag_id}"
    elif msg_type_str == "afm":  # add_forward_message
        article_id = specific_field_val
        return f"afm {message_id} {article_id} {msg_delivery_type} {person_id1} {person_id2_or_tag_id}"
    return None


def generate_add_message():
    social_value = random.randint(MIN_SOCIAL_VALUE, MAX_SOCIAL_VALUE)
    return generate_add_message_base("am", social_value, "social_value")


def generate_add_emoji_message():
    # Emoji ID can be new or existing from network_stored_emoji_ids
    emoji_id = None
    if network_stored_emoji_ids and random.random() < 0.6:
        emoji_id = random.choice(list(network_stored_emoji_ids))
    else:
        emoji_id = generate_random_id(min_val=MIN_EMOJI_ID, max_val=MAX_EMOJI_ID)
    return generate_add_message_base("aem", emoji_id, "emoji_id")


def generate_add_red_envelope_message():
    money = random.randint(0, MAX_RED_ENVELOPE_MONEY)
    return generate_add_message_base("arem", money, "money")


def generate_add_forward_message():
    if not all_article_ids: return None  # Need articles to forward
    # article_id for afm should be an existing article from the simulation perspective
    article_id = get_id_for_command(id_type='article', use_existing_prob=0.95, specific_set_to_use=all_article_ids)
    if article_id is None and all_article_ids:
        article_id = random.choice(list(all_article_ids))  # Ensure one if available
    elif article_id is None:
        return None

    return generate_add_message_base("afm", article_id, "article_id")


def generate_send_message():
    if not existing_message_ids: return None
    # Message ID should be one that was added
    message_id_to_send = get_id_for_command(id_type='message', use_existing_prob=0.95)
    # Actual sending logic (money transfer, social value update, etc.) is handled by JARs.
    # We just generate the command.
    # If message_id was sent, it's removed from network.messages. We don't strictly track this here
    # to allow testing resending (which might be an error or specific behavior).
    # For simplicity, once a message is "sent" by this command, we could remove it from
    # existing_message_ids to prevent multiple "sm" for the same message id in a test run,
    # or allow it to test robustness. Let's allow it.
    return f"sm {message_id_to_send}"


def generate_store_emoji_id():
    emoji_id = generate_random_id(min_val=MIN_EMOJI_ID, max_val=MAX_EMOJI_ID)
    # Try to add a new one more often
    if random.random() < 0.3 and network_stored_emoji_ids:  # Small chance to re-store
        emoji_id = random.choice(list(network_stored_emoji_ids))

    network_stored_emoji_ids.add(emoji_id)
    return f"sei {emoji_id}"


def generate_delete_cold_emoji():
    limit = random.randint(0, 100)  # Example limit range
    # State change (emoji deletion) is handled by JAR.
    # We don't need to update network_stored_emoji_ids here based on dce.
    return f"dce {limit}"


def generate_query_social_value():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    return f"qsv {person_id}"


def generate_query_received_messages():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    return f"qrm {person_id}"


def generate_query_popularity():  # This is for emoji popularity
    if not network_stored_emoji_ids:  # If no emojis ever stored, query might be less meaningful
        # Querying a random emoji ID to test cases where emoji might not exist or have 0 heat
        emoji_id_to_query = generate_random_id(min_val=MIN_EMOJI_ID, max_val=MAX_EMOJI_ID)
    else:
        emoji_id_to_query = get_id_for_command(id_type='emoji_stored', use_existing_prob=0.9)
    return f"qp {emoji_id_to_query}"


def generate_query_money():
    if not existing_person_ids: return None
    person_id = get_id_for_command(id_type='person', use_existing_prob=0.9)
    return f"qm {person_id}"


# --- Updated Test Data Generation ---
def generate_test_data(num_commands):
    """生成包含指定数量指令的测试数据列表"""
    # Clear state for the new test case
    existing_person_ids.clear()
    existing_tags.clear()
    existing_relations.clear()
    tag_members.clear()
    existing_account_ids.clear()
    account_owners.clear()
    existing_articles.clear()
    all_article_ids.clear()
    articles_metadata.clear()
    account_followers.clear()
    person_followed_accounts.clear()
    # HW3 state
    existing_message_ids.clear()
    network_stored_emoji_ids.clear()
    message_details.clear()
    # person_money.clear()
    # person_social_value.clear()

    commands = []
    # --- Initial Population Phase ---
    # More initial entities for HW3 to make message commands meaningful
    num_initial_persons = min(30, num_commands // 50)
    for _ in range(num_initial_persons):
        cmd = generate_add_person();
        if cmd: commands.append(cmd)

    num_initial_relations = min(60, len(existing_person_ids) * (len(existing_person_ids) - 1) // 3 if len(
        existing_person_ids) > 1 else 0)
    for _ in range(num_initial_relations):
        cmd = generate_add_relation();
        if cmd: commands.append(cmd)

    num_initial_tags = min(40, len(existing_person_ids) * 2)
    for _ in range(num_initial_tags):
        cmd = generate_add_tag();
        if cmd: commands.append(cmd)

    num_initial_accounts = min(20, len(existing_person_ids))
    for _ in range(num_initial_accounts):
        cmd = generate_create_official_account();
        if cmd: commands.append(cmd)

    num_initial_articles = min(50, len(existing_account_ids) * 3)
    for _ in range(num_initial_articles):
        cmd = generate_contribute_article();
        if cmd: commands.append(cmd)

    num_initial_follows = min(80, (len(existing_person_ids) * len(
        existing_account_ids)) // 2 if existing_account_ids else 0)
    for _ in range(num_initial_follows):
        cmd = generate_follow_official_account();
        if cmd: commands.append(cmd)

    # Initial stored emojis
    num_initial_stored_emojis = min(15, num_commands // 100)
    for _ in range(num_initial_stored_emojis):
        cmd = generate_store_emoji_id()
        if cmd: commands.append(cmd)

    # Initial messages (add some but don't send yet)
    num_initial_messages = min(50, num_commands // 50)
    initial_message_generators = [
        generate_add_message, generate_add_emoji_message,
        generate_add_red_envelope_message, generate_add_forward_message
    ]
    for _ in range(num_initial_messages):
        gen_func = random.choice(initial_message_generators)
        cmd = gen_func()
        if cmd: commands.append(cmd)

    command_generators = {
        # HW1 Commands
        'ap': generate_add_person, 'ar': generate_add_relation, 'mr': generate_modify_relation,
        'at': generate_add_tag, 'dt': generate_del_tag, 'att': generate_add_to_tag,
        'dft': generate_del_from_tag, 'qv': generate_query_value, 'qci': generate_query_circle,
        'qts': generate_query_triple_sum, 'qtav': generate_query_tag_age_var,
        'qba': generate_query_best_acquaintance,
        # HW2 Commands
        'coa': generate_create_official_account, 'doa': generate_delete_official_account,
        'ca': generate_contribute_article, 'da': generate_delete_article,
        'foa': generate_follow_official_account, 'qsp': generate_query_shortest_path,
        'qbc': generate_query_best_contributor, 'qra': generate_query_received_articles,
        'qtvs': generate_query_tag_value_sum, 'qcs': generate_query_couple_sum,
        # HW3 Commands
        'am': generate_add_message, 'aem': generate_add_emoji_message,
        'arem': generate_add_red_envelope_message, 'afm': generate_add_forward_message,
        'sm': generate_send_message, 'sei': generate_store_emoji_id,
        'dce': generate_delete_cold_emoji, 'qsv': generate_query_social_value,
        'qrm': generate_query_received_messages, 'qp': generate_query_popularity,
        'qm': generate_query_money,
    }
    command_types = list(command_generators.keys())

    # Approximate weights, trying to balance old and new
    # HW1: 12 types, HW2: 10 types, HW3: 11 types. Total 33.
    # Let's aim for roughly equal distribution among the three homeworks' functionalities.
    # Sum of weights should be around 100 for random.choices.
    # HW1 weights (sum ~30-35)
    weights = [2, 3, 2, 2, 1, 2, 1, 3, 2, 1, 3, 2,  # ap ar mr at dt att dft qv qci qts qtav qba
               # HW2 weights (sum ~30-35)
               2, 1, 3, 2, 3, 3, 3, 3, 3, 2,  # coa doa ca da foa qsp qbc qra qtvs qcs
               # HW3 weights (sum ~30-35)
               3, 3, 3, 3, 4, 2, 2, 3, 3, 3, 3  # am aem arem afm sm sei dce qsv qrm qp qm
               ]

    while len(commands) < num_commands:
        if len(weights) != len(command_types):
            print(
                f"Warning: Mismatch between command types ({len(command_types)}) and weights length ({len(weights)}). Using uniform random choice.")
            cmd_type = random.choice(command_types)
        else:
            cmd_type = random.choices(command_types, weights=weights, k=1)[0]

        generator = command_generators[cmd_type]
        command = generator()
        if command:  # Make sure generator produced a command
            commands.append(command)
        # else:
        # print(f"Generator for {cmd_type} returned None.")

    # print(f"Generated {len(commands)} commands.")
    return commands


# --- JAR 执行与比对 (No changes needed below this line generally) ---

def run_jar(jar_path, input_data, output_queue, jar_index, commands, results_dict, test_count):
    """在单独的进程中运行 JAR 文件并获取输出"""
    try:
        process = subprocess.Popen(
            ['java', '-jar', jar_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,  # 捕获错误输出
            text=True,
            # Ensure consistent encoding, though Popen's default text=True usually handles UTF-8 well.
            # Explicitly setting encoding might be needed if issues arise.
            # encoding='utf-8', # Try adding if encoding issues with stdout/stderr occur
            # errors='ignore' # Or handle encoding errors if they are problematic
        )
        stdout, stderr = process.communicate(input=input_data, timeout=TIMEOUT_SECONDS)
        # print(f"JAR {jar_index+1} ({os.path.basename(jar_path)}) stderr:\n{stderr}") # Debug: show stderr
        output_queue.put((jar_index, stdout.strip().splitlines()))
    except subprocess.TimeoutExpired:
        process.kill()  # Ensure process is killed
        # Try to communicate again to get any final output/error
        stdout_after_kill, stderr_after_kill = process.communicate()
        print(f"Error: JAR {jar_index + 1} ({os.path.basename(jar_path)}) timed out.")
        # print(f"  Stderr after timeout kill for {os.path.basename(jar_path)}:\n{stderr_after_kill}") # Debug
        output_queue.put((jar_index, ["TIMEOUT_ERROR"]))
        current_test_case_dir = create_test_case_dir(test_count)
        write_output_files(current_test_case_dir, commands, results_dict)
    except FileNotFoundError:
        print(f"Error: Java command not found. Make sure Java is installed and in your PATH.")
        output_queue.put((jar_index, ["JAVA_NOT_FOUND_ERROR"]))
        # This is a fatal error for this script, so maybe exit or handle differently
    except Exception as e:
        print(f"Error running JAR {jar_index + 1} ({os.path.basename(jar_path)}): {e}")
        output_queue.put((jar_index, [f"EXECUTION_ERROR: {str(e).splitlines()[0]}"]))  # Keep error brief


def write_output_files(test_case_dir, input_commands, outputs_dict):
    """将输入和每个JAR的输出写入单独的文件"""
    # 写入输入文件
    with open(os.path.join(test_case_dir, "Input.txt"), 'w', encoding='utf-8') as f:
        f.write("\n".join(input_commands))

    # 写入每个JAR的输出文件
    for i in range(NUM_JARS):  # Iterate up to NUM_JARS to handle missing outputs
        output_lines = outputs_dict.get(i, [f"NO_OUTPUT_CAPTURED_FOR_JAR_{i + 1}"])  # Default if no output
        jar_name = os.path.basename(JAR_FILES[i]) if i < len(JAR_FILES) else f"Unknown_JAR_{i + 1}"
        with open(os.path.join(test_case_dir, f"result_{jar_name}.txt"), 'w', encoding='utf-8') as f:
            f.write("\n".join(output_lines))


def write_log(test_case_dir, input_commands, outputs_dict):
    """将错误信息写入日志文件"""
    log_path = os.path.join(test_case_dir, "error_summary_log.txt")  # More descriptive name
    with open(log_path, 'w', encoding='utf-8') as f:
        f.write("=" * 50 + "\n")
        f.write(f"Test Case Directory: {os.path.basename(test_case_dir)}\n")
        f.write(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("Input Commands:\n")
        f.write("\n".join(input_commands))
        f.write("\n\n")
        f.write("Outputs Comparison:\n")

        if not outputs_dict or 0 not in outputs_dict:
            f.write("Error: No output from the reference JAR (JAR 1) or no outputs at all.\n")
            for i in range(NUM_JARS):
                jar_name = os.path.basename(JAR_FILES[i]) if i < len(JAR_FILES) else f"Unknown_JAR_{i + 1}"
                f.write(f"--- Output from JAR {i + 1} ({jar_name}) ---\n")
                f.write("\n".join(outputs_dict.get(i, ["NO_OUTPUT"])))
                f.write("\n")
            f.write("=" * 50 + "\n\n")
            return

        ref_output = outputs_dict[0]
        f.write(f"--- Reference Output (JAR 1: {os.path.basename(JAR_FILES[0])}) ---\n")
        f.write("\n".join(ref_output))
        f.write("\n\n")

        for i in range(1, NUM_JARS):
            jar_name = os.path.basename(JAR_FILES[i]) if i < len(JAR_FILES) else f"Unknown_JAR_{i + 1}"
            current_output = outputs_dict.get(i)
            f.write(f"--- Output from JAR {i + 1} ({jar_name}) ---\n")
            if current_output is None:
                f.write("NO_OUTPUT_CAPTURED\n")
            else:
                f.write("\n".join(current_output))
            f.write("\n")

            if current_output != ref_output:
                f.write(f"MISMATCH with JAR 1!\n")
                # Add more detailed diff if desired here
            f.write("\n")
        f.write("=" * 50 + "\n\n")


# --- 主逻辑 ---

if __name__ == "__main__":
    # ensure_output_dir() # Create base output directory
    if not all(os.path.exists(jar) for jar in JAR_FILES):
        print("Error: One or more JAR files specified in JAR_FILES do not exist.")
        print("Please check the paths:")
        for jar_path_check in JAR_FILES:  # Renamed variable to avoid conflict
            if not os.path.exists(jar_path_check):
                print(f"- {jar_path_check}")
        exit(1)
    if NUM_JARS == 0:
        print("Error: No JAR files specified in JAR_FILES. Exiting.")
        exit(1)

    test_count = 0
    error_count = 0
    print(f"Starting parallel testing with {NUM_JARS} JAR files...")
    print(f"Timeout per JAR: {TIMEOUT_SECONDS}s. Commands per test: {NUM_COMMANDS_PER_TEST}.")
    print(f"Outputting results to: {OUTPUT_DIR}")

    try:
        while True:  # Loop for multiple test cases
            test_count += 1
            # current_test_case_dir = create_test_case_dir(test_count) # Create dir for this test
            print(f"\n--- Running Test Case {test_count} ---")

            commands = generate_test_data(NUM_COMMANDS_PER_TEST)
            if not commands:
                print("Failed to generate any commands. Skipping test case.")
                continue
            input_str = "\n".join(commands)

            output_queue = Queue()
            threads = []
            results_dict = {}  # Use a dictionary {jar_index: output_lines}

            start_time = time.time()

            for i, jar_file_to_run in enumerate(JAR_FILES):  # Renamed variable
                thread = threading.Thread(target=run_jar,
                                          args=(jar_file_to_run, input_str, output_queue, i, commands, results_dict,
                                                test_count))
                threads.append(thread)
                thread.start()

            # Wait for all threads to complete
            for thread in threads:
                thread.join()  # This is simpler and ensures all threads finish

            end_time = time.time()
            print(f"Test Case {test_count} execution time: {end_time - start_time:.2f} seconds")

            # Collect results from queue
            while not output_queue.empty():
                jar_idx, output_lines_from_q = output_queue.get()  # Renamed variable
                results_dict[jar_idx] = output_lines_from_q

            # Write all outputs to files first
            # write_output_files(current_test_case_dir, commands, results_dict)

            # --- Comparison Logic ---
            inconsistent_found = False
            if 0 not in results_dict:  # Reference JAR failed or no output
                print(
                    f"Error: Reference JAR (JAR 1: {os.path.basename(JAR_FILES[0])}) did not produce output or failed.")
                inconsistent_found = True
            else:
                ref_output_lines = results_dict[0]
                for i in range(1, NUM_JARS):
                    current_output_lines = results_dict.get(i)
                    if current_output_lines is None:
                        print(f"Warning: JAR {i + 1} ({os.path.basename(JAR_FILES[i])}) did not produce output.")
                        inconsistent_found = True
                        break  # Mismatch found
                    if current_output_lines != ref_output_lines:
                        print(
                            f"Inconsistency detected between JAR 1 and JAR {i + 1} ({os.path.basename(JAR_FILES[i])})!")
                        inconsistent_found = True
                        break  # Mismatch found

            if inconsistent_found:
                error_count += 1
                current_test_case_dir = create_test_case_dir(test_count)
                print(f"Details saved to: {current_test_case_dir}")
                write_output_files(current_test_case_dir, commands, results_dict)
                write_log(current_test_case_dir, commands, results_dict)  # Write detailed log on error
            else:
                print("Outputs are consistent for this test case.")
                # Optionally, clean up non-error directories if desired, or keep all
                # For now, keep all for inspection.

    except KeyboardInterrupt:
        print("\n--- Testing Interrupted by User ---")
    except Exception as e:
        print(f"\n--- An unexpected error occurred in the judge script: {e} ---")
        import traceback

        traceback.print_exc()
    finally:
        print(f"\n--- Testing Finished ---")
        print(f"Total test cases run: {test_count}")
        print(f"Inconsistencies / Failures logged: {error_count}")
        if error_count > 0:
            print(f"Check the subdirectories in '{OUTPUT_DIR}' for details of failed tests.")
        else:
            print(f"All run test cases were consistent (if any errors occurred before comparison, check console).")
