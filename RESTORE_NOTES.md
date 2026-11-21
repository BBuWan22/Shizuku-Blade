# 源码还原说明（3.7.0）

本分支的 `src/` 是从**已发布的 `shizuku-blade-3.7.0.jar`（Forge 1.20.1）反编译还原**出来的，
用于在原源码丢失后找回一份可继续开发、可编译的代码。**游戏逻辑一行未改**，
只做了「让反编译结果变回正常 Java 源码」所必需的修正，并补上了中文注释。

## 还原流程（可复现）

1. **脱 SRG**：1.20.1 的 Forge 会把手里的 jar 重混淆成 SRG 名（`m_xxxxx_` / `f_xxxxx_`），
   直接反编译只能得到 `m_91087_()` 这种不可读调用。先用 MCPConfig 的 `joined.tsrg`
   与 Mojang 官方映射（`client_mappings`）按混淆名做一次连接，得到 **SRG → 官方名** 的全量对照表
   （SRG id 全局唯一，因此按名字替换即可，无需沿继承链解析），把 jar 里的成员名整体换回官方名。
2. **反编译**：Vineflower 1.11.2（`-dgs=1`，带 JRT）。
   该 jar 编译时带 `-g`，所以**局部变量名与行号信息完整保留**，还原出来的代码非常接近原始源码。
3. **资源**：`assets/`、`data/`、`shizuku.mixins.json` 全部取自 jar 本体（以发布版为准）。
4. **注释**：每个类补了类级说明（作用、关键参数、已知问题），关键流程（渲染/SA/清怪/配方）另加说明。

## 与 jar 内容不同的地方（全部为「反编译产物修正」，不改变行为）

| 位置 | 修正 | 原因 |
| --- | --- | --- |
| `mixin/*`（6 个类） | `(X)this` → `(X)(Object)this` | 反编译器丢掉了中间那次 `Object` 转型，mixin 抽象类里不转型无法通过编译 |
| `core/UltimateAttackOHelp` | `ArrayList` → `ArrayList<Field>`；lambda 里 `field` → `fieldx` | 反编译丢泛型 + 循环变量被重命名，编译不过 |
| `core/UltimateEntityRemover` | `instanceof Map<Integer,Object>` → `Map<?,?>`；`Collection` → `Collection<ServerPlayer>` | 泛型不可 reify（`instanceof` 报错）；裸 `Collection` 的 lambda 参数被当成 `Object` |
| `gradle.properties` | `mod_version` 1.0.0 → **3.7.0** | 与已发布 jar 对齐；不想要可以改回去 |

## 刻意保留的已知问题（未修，仅注释标注）

- `mixin/EntityAbstractSummonedSwordMixin`：本体 `inBlockState` 可能为 `null`，`tick()` 里直接
  `equals()` 会 NPE（任何「还没撞过方块」就被存档的刀光，读档后必崩）。这里的 `HEAD + cancel`
  写法会让该字段保持 null，反而更容易触发。
- `client/SynthwaveShaderManager`：合成波没有淡入，首次开启是瞬跳满强度；快速连点右键会反复
  开关，表现为脚下持续闪烁。
- `jei/.../ShizukuCraftingTableCategory`：JEI 输入槽里的命名刀只能显示成无铭占位刀
  （配料的 `request` 拿不到）。
- `core/HealthLockManager` 与 `util/HealthLockManager` 两份同源实现并存，改一处容易漏另一处。
- `crafting/ShapelessTableCraftingRecipe`：产物不支持「指定命名刀」（不解析 `blade`）。

## 验证

- `gradlew build` 应能直接通过（ForgeGradle 会自动应用 Forge 的 access transformer，
  `RenderStateShard` 那些 protected 内部类在开发环境里是可用的）。
- 编译产物与发布 jar 的类数量应一致（178 个 class / 136 个 java 文件）。
- 行为验证：进游戏拿星河刀砍、开时停/囚牢、看 JEI 工作台配方，应与 3.7.0 发布版一致。

## 许可

本仓库为 LGPL-3.0。这份源码就是本仓库自身的代码（由发布 jar 还原而来），未引入任何第三方代码。
