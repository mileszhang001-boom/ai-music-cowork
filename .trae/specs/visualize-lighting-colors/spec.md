# 调试工具灯光颜色可视化 Spec

## Why
当前调试工具显示灯光色值时，小圆点使用固定的红色和黄色，无法直观展示实际颜色。用户希望小圆点的颜色直接对应实际的色值，以便更直观地看到灯光效果。

## What Changes
- 将 HEX 色值转换为终端 ANSI 256 色彩码
- 小圆点 `●` 使用实际色值对应的颜色显示
- 支持 RGB 颜色到终端颜色的近似映射

## Impact
- Affected code: `scripts/debug.js`
- 影响灯光显示模块

## ADDED Requirements

### Requirement: 灯光颜色可视化
调试工具 SHALL 将 HEX 色值转换为终端颜色码，使小圆点显示实际颜色。

#### Scenario: 显示灯光颜色
- **WHEN** 调试工具显示灯光主色调和辅助色
- **THEN** 小圆点 `●` 的颜色应直接对应 HEX 色值

#### 示例
```
💡 灯光:
  主题: cool (40%)
  主色调: ● #00BCD4 (青色)    ← 小圆点显示青色
  辅助色: ● #3F51B5 (靛蓝)    ← 小圆点显示靛蓝色
```

## MODIFIED Requirements

### Requirement: 颜色显示函数
新增 `hexToAnsi(hex)` 函数，将 HEX 颜色转换为终端 ANSI 256 色彩码：

```javascript
function hexToAnsi(hex) {
  // 将 HEX 转换为 RGB
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  
  // 转换为 ANSI 256 色彩码
  // 使用 6x6x6 立方体颜色空间
  return `\x1b[38;5;${ansi256}m`;
}
```
