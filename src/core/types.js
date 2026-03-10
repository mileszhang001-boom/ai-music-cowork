'use strict';

/**
 * @fileoverview 核心类型定义 - 车载座舱 AI 娱乐融合方案
 * @description 定义系统中使用的所有核心数据结构和接口
 */

/**
 * @typedef {Object} Signal - 原始信号
 * @property {string} source - 信号来源 (vhal|voice|biometric|environment|user_profile|music_state)
 * @property {string} type - 信号类型
 * @property {*} value - 信号值
 * @property {number} timestamp - 时间戳 (ms)
 * @property {number} [confidence=1.0] - 置信度 (0-1)
 * @property {Object} [metadata] - 元数据
 */

/**
 * @typedef {Object} NormalizedSignal - 标准化信号
 * @property {string} signal_id - 信号唯一标识
 * @property {string} source - 信号来源
 * @property {string} category - 信号类别 (context|user_state|user_intent|environment)
 * @property {*} value - 标准化后的值
 * @property {number} confidence - 置信度 (0-1)
 * @property {number} timestamp - 时间戳 (ms)
 * @property {number} ttl - 生存时间 (ms)
 */

/**
 * @typedef {Object} SceneVector - 场景向量
 * @property {string} scene_type - 场景类型
 * @property {Object} dimensions - 场景维度
 * @property {number} dimensions.social - 社交维度 (0=solo, 1=group)
 * @property {number} dimensions.energy - 能量维度 (0=calm, 1=excited)
 * @property {number} dimensions.focus - 专注维度 (0=relaxed, 1=focused)
 * @property {number} dimensions.time_context - 时间上下文 (0=night, 1=day)
 * @property {number} dimensions.weather - 天气影响 (0=clear, 1=adverse)
 * @property {number} confidence - 场景识别置信度
 * @property {number} timestamp - 时间戳 (ms)
 */

/**
 * @typedef {Object} Mood - 情绪状态
 * @property {number} valence - 效价 (0=negative, 1=positive)
 * @property {number} arousal - 唤醒度 (0=calm, 1=excited)
 */

/**
 * @typedef {Object} EnergyCurvePoint - 能量曲线点
 * @property {number} time_offset_s - 时间偏移 (秒)
 * @property {number} energy_level - 能量级别 (0-1)
 */

/**
 * @typedef {Object} Constraints - 约束条件
 * @property {number} [max_volume_db] - 最大音量 (dB, 负值表示相对衰减)
 * @property {boolean} [avoid_vocal] - 是否避免人声
 * @property {boolean} [avoid_explicit] - 是否避免敏感内容
 * @property {string[]} [preferred_genres] - 偏好流派
 * @property {string[]} [blocked_artists] - 屏蔽艺人
 * @property {number} [max_duration_sec] - 最大时长 (秒)
 */

/**
 * @typedef {Object} UserOverrides - 用户覆盖
 * @property {string} [preferred_genre] - 用户指定流派
 * @property {string} [preferred_artist] - 用户指定艺人
 * @property {string} [preferred_mood] - 用户指定情绪
 * @property {string} [specific_song] - 用户指定歌曲
 */

/**
 * @typedef {Object} Transition - 过渡策略
 * @property {string} type - 过渡类型 (fade|cut|event_driven|urgent)
 * @property {number} [duration_ms] - 过渡时长 (ms)
 * @property {string} [trigger_event] - 触发事件
 */

/**
 * @typedef {Object} Intent - 意图描述
 * @property {Mood} mood - 情绪状态
 * @property {number} energy_level - 能量级别 (0-1)
 * @property {EnergyCurvePoint[]} [energy_curve] - 能量曲线
 * @property {string} atmosphere - 氛围描述
 * @property {Constraints} [constraints] - 约束条件
 * @property {UserOverrides} [user_overrides] - 用户覆盖
 * @property {Transition} [transition] - 过渡策略
 */

/**
 * @typedef {Object} MusicHints - 音乐提示
 * @property {string[]} [genres] - 流派建议
 * @property {string[]} [artists] - 艺人建议
 * @property {string[]} [eras] - 年代建议
 * @property {string} [tempo] - 节奏建议 (slow|medium|fast|variable)
 * @property {string} [vocal_style] - 人声风格 (instrumental|male|female|duet|any)
 * @property {string} [language] - 语言偏好
 */

/**
 * @typedef {Object} LightingHints - 灯光提示
 * @property {string} color_theme - 颜色主题
 * @property {string} pattern - 动效模式 (static|breathing|pulse|wave|music_sync)
 * @property {number} [intensity] - 亮度 (0-1)
 * @property {string} [transition] - 过渡效果
 */

/**
 * @typedef {Object} AudioHints - 音效提示
 * @property {string} preset - 音效预设
 * @property {string} [spatial_mode] - 空间音频模式
 * @property {number} [bass_boost] - 低音增强 (0-1)
 * @property {number} [treble_boost] - 高音增强 (0-1)
 */

/**
 * @typedef {Object} Hints - 提示集合
 * @property {MusicHints} [music] - 音乐提示
 * @property {LightingHints} [lighting] - 灯光提示
 * @property {AudioHints} [audio] - 音效提示
 */

/**
 * @typedef {Object} Announcement - 语音播报
 * @property {string} text - 播报文本
 * @property {string} voice_style - 语音风格
 * @property {string} [trigger] - 触发条件
 * @property {number} [delay_ms] - 延迟 (ms)
 */

/**
 * @typedef {Object} SceneDescriptorMeta - 场景描述元数据
 * @property {number} created_at - 创建时间戳
 * @property {string} source - 来源 (template|llm|hybrid)
 * @property {number} [template_id] - 模板ID
 * @property {number} [confidence] - 置信度
 * @property {string} [reasoning] - 推理过程
 */

/**
 * @typedef {Object} SceneDescriptor - 场景描述符 V2.0
 * @property {string} version - 版本号 "2.0"
 * @property {string} scene_id - 场景唯一标识
 * @property {string} [scene_name] - 场景名称
 * @property {string} [scene_narrative] - 场景叙事描述
 * @property {Intent} intent - 意图描述
 * @property {Hints} [hints] - 提示集合
 * @property {Announcement} [announcement] - 语音播报
 * @property {SceneDescriptorMeta} [meta] - 元数据
 */

/**
 * @typedef {Object} ACKMessage - ACK 消息
 * @property {string} type - 消息类型 "ack"
 * @property {string} text - 播报文本
 * @property {string} voice_style - 语音风格
 * @property {string} timestamp - 时间戳 (ISO 8601)
 * @property {string} query_intent - 查询意图 (creative|navigation|control|info)
 * @property {string} [related_query] - 关联查询
 * @property {number} [estimated_wait_sec] - 预计等待时间 (秒)
 */

/**
 * @typedef {Object} FeedbackReport - 反馈报告
 * @property {string} report_id - 报告唯一标识
 * @property {string} scene_id - 关联场景ID
 * @property {string} timestamp - 时间戳 (ISO 8601)
 * @property {string} type - 反馈类型 (explicit|implicit)
 * @property {Object} details - 反馈详情
 * @property {string} [details.action] - 用户动作
 * @property {string} [details.target] - 目标对象
 * @property {number} [details.value] - 数值变化
 * @property {string} [details.reason] - 原因
 * @property {number} [satisfaction_score] - 满意度评分 (1-5)
 */

/**
 * @typedef {Object} Layer1Output - Layer 1 输出
 * @property {string} output_id - 输出唯一标识
 * @property {number} timestamp - 时间戳 (ms)
 * @property {NormalizedSignal[]} signals - 标准化信号列表
 * @property {Object} signal_summary - 信号摘要
 * @property {number} signal_summary.total_count - 总信号数
 * @property {number} signal_summary.high_confidence_count - 高置信度信号数
 * @property {string[]} signal_summary.active_sources - 活跃信号源
 */

/**
 * @typedef {Object} Layer2Output - Layer 2 输出
 * @property {string} output_id - 输出唯一标识
 * @property {number} timestamp - 时间戳 (ms)
 * @property {SceneVector} scene_vector - 场景向量
 * @property {Object} change_detection - 变化检测
 * @property {boolean} change_detection.scene_changed - 场景是否变化
 * @property {string} [change_detection.previous_scene] - 前一场景
 * @property {string} [change_detection.change_type] - 变化类型
 * @property {number} [change_detection.change_magnitude] - 变化幅度
 */

/**
 * @typedef {'vhal'|'voice'|'biometric'|'environment'|'user_profile'|'music_state'} SignalSource
 * @description 信号来源类型
 */

/**
 * @typedef {'context'|'user_state'|'user_intent'|'environment'} SignalCategory
 * @description 信号类别
 */

module.exports = {};
