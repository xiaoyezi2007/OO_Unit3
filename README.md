# README

- 使用方式：

  该测评机基于对拍实现，将需要进行对拍的jar包放在JAR目录下（不放这自行配置也行），修改JAR_FILES，按照注释修改路径为自己的jar包名字，运行即可，若出现不同则会产生results目录，存储了测试点编号以及错误的input及每个jar包的输出

- 参数设置：

  ```python
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
  ```

- tips：只展示了第三次迭代的测评机，如果需要对前置进行测试，则自行注释掉相应指令的生成函数即可（函数名意义明确，看名字即可知道对应什么指令）