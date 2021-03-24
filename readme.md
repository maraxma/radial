# radial
radial是为spring boot开发的多数据源支持工具。radial目前仅支持druid和hikariCP两种类型的数据源实现。

radial目前支持两种形式的“多数据源”：

- **可切换数据源**：在整个Spring容器里只保持一个DataSource的实例，在运行时可以使用@UseDataSource切换。这种形式与Spring的其它框架兼容性好，因为其它的框架大都认定Spring容器里只有一个数据源，如mybatis框架。推荐使用这种形式。
- **多重数据源**：在整个Spring容器里保存多个数据源（你配置的所有数据源），在运行时使用@Autowired或者@Resource指定某一个。这种形式更符合“多”的概念，可以自由使用，但是和其他的框架兼容性不好，这种形式仅推荐在有特殊需求的时候使用。

# 使用

1、向你的工程中加入依赖：

```xml
<dependency>
	<groupId>com.newegg.mkpl</groupId>
	<artifactId>radial</artifactId>
	<version>version</version>
</dependency>
```

2、在你的spring配置文件中配置多个数据源：

```yml
radial:
  datasources:
    DS1: 
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      url: jdbc:sqlserver://XXXXX
      type: com.zaxxer.hikari.HikariDataSource // 只能是Druid或者hikariCP
      hikari: // 是hikiriCP就写hikari，是druid就写druid，到目前为止只支持这两种
        maximum-pool-size: 5
        auto-commit: false
        idle-timeout: 30000
        pool-name: HikariConnectionPool
        max-lifetime: 1800000
        connection-timeout: 30000
        connection-test-query: "SELECT 1"

    DS2: 
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      url: jdbc:sqlserver://XXX
      type: com.zaxxer.hikari.HikariDataSource
      hikari:
        maximum-pool-size: 5
        auto-commit: false
        idle-timeout: 30000
        pool-name: HikariConnectionPool
        max-lifetime: 1800000
        connection-timeout: 30000
        connection-test-query: "SELECT 1"
      
    DSXXX: 
      ......

```
3、在你的入口类上加上如下的注解以启用它的功能：

- @EnableSwitchableDataSource：启用可切换数据源，推荐。
- @EnableMultiDataSource：启用多重数据源，特殊情况下可考虑使用。**在使用它的时候请再在入口类上附加“@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})”以屏蔽自带的DataSource初始化程序，否则会报错。**
- @EnableSwitchableDataSourceOnMapper：启用后，将可以在MyBatis Mapper接口的抽象方法上使用@UseDataSource注解。仅支持可切换数据源。

注意：上述的两个注解是冲突的，若两个都存在，那么只会认为@EnableSwitchableDataSource有效。

4、指定数据源：

- 当使用可切换数据源（@EnableSwitchableDataSource）时：使用@UseDataSource注解。该注解能用在方法和成员变量上。使用@UseDataSource("XXX")来指定你的数据源。
- 当使用多数据源（@EnableMultiDataSource）时：使用@UseDataSource、@Resource。@UseDataSource注解只能用在成员变量上，在这种情况下它等同于@Resource(name = "XXX")。在成员变量上使用@UseDataSource("XXX")或者@Resource(name= "XX")来指定你的数据源，在方法上使用它将没有任何效果。

"XX"代表数据源名称。数据源名称是你配置的名称前面附加"dataSource_"，如"dataSource_NEWSQL"。


5、开始在代码中使用

```java
// 如需在方法上切换：
// XXServiceImpl.java:
@UseDataSource("dataSource_DS1") // 可切换数据源专有用法
public Map<String, Object> findPeople(int id) {
	return XXMapper.find(id);
}

// 如需直接使用dataSource：
@Autowired  // 可切换数据源只有一个，因此可以直接使用@Autowired
private SwitchableDataSource dataSource;

// 如需在运行时获得指定数据源：（可切换数据源专有用法）
dataSource.getDataSource("dataSource_NEWSQL").XXXX();

// 如果需要直接指定某个数据源给成员变量（自动注入，时候不要再使用@Autowired或者@Resource）
@UseDataSource("dataSource_DS2")
private DataSource dataSource;

// 查看已被管理的数据源名称（方便你通过名称获取它们，可切换数据源专有用法）：
dataSource.getAvailableDataSourceNames();

// 在MyBatis Mapper接口方法上使用（需要预先启用@EnableSwitchableDataSourceOnMapper）
@Mapper
public interface UserMapper {
    @UseDataSource("dataSource_DS1")
    UserModel getUserInfo(int id);
}
```

6、启用事务

```java
// MyServiceImpl.java
@Override
@UseDataSource("dataSource_MKTPLS")
@Transactional
public void update() {
    // 业务逻辑，支持跨数据源的事务
    studentMapper.updateScore(1, 99);
    courseMapper.updateCourse(1);
    // 其他业务逻辑
    // 当这个方法在任意行出现异常时radial会帮助你将所有的更新回滚
}
```

注意，目前radial仅实现了简单的事务支持，暂不支持除默认Propagation.REQUIRED事务嵌套类型外的其他类型。
且必须配合SwitchableDataSourceTransactionManager一起使用，不能更改@Transaction.transactionManager。

# 附加的功能

## 自定义数据源命名策略

实现DataSourceNamingStrategy接口，并将它交给spring管理。实现你自己的命名策略后你在需要引用数据源的地方需要填入你自己的命名。

## 停用某个数据源

在spring配置的指定数据源配置中加入"active: false"即可。

## MXBean

你可以使用JMX或者JMC连接到本地或者远程JVM实例观看我们提供的MXBean，这个MXBean可以帮助你了解你正在运行的程序中你的可切换数据源配置了哪些具体的数据源以及连接池的运行状况（启用了多少连接、哪些连接池在运行等）。

## 为Mapper接口的抽象方法指定数据源

很多时候我们期望在稍微底层一点的方法上标注@UseDataSource以指定一个SQL语句对应的数据源，这时你只需要在入口类上标注@EnableSwitchableDataSourceOnMapper，然后将你的@UseDataSource标注在Mapper的方法上即可。如：

```java
// MyMapper.java (interface)
@Mapper
public interface TestMapper {
	@UseDataSource("dataSource_HisQuery") void insert(String text);
}
```

**你需要注意的是，当你以上面的方法指定了数据源后，数据源便和那个方法绑定了，在任何时候都无法再进行切换。**

# 错误排查

radial只负责将你的数据源配置转换为被Spring管理的DataSource，因此生成了DataSource后radial就完成了它的使命，
radial不能也无法继续追踪后续DataSource的使用情况。因此，如果你在使用radial后报出了错误，
但是你又希望知道当前到底是哪个数据源报的错，你应该自行实现如下的操作：

- 知道数据处理经过了哪些框架
- 捕获SQL执行语句（准确的说是execute），将异常抓住，并在catch块里打出当前的数据源名称

例子：

你的项目现在报出了如下的错误：Caused by: com.microsoft.sqlserver.jdbc.SQLServerException: Invalid object name 'DBCenter.dbo.UserInfo'.

1、分析数据处理流程

radial --> 生成SwitchableDataSource --> DataSource交由MyBatis --> MyBatis调用DataSource.getConnection()方法获得连接 --> MyBatis使用该连接执行SQL语句

2、知道了最终的处理是MyBatis，那么捕获MyBatis的Mapper的具体方法，在catch块中打印信息

```java
try {
	userMapper.get(userId);
} catch (Exception e) {
	LOGGER.error("Exception captured when executing SQL in datasource " + DataSourceContextHolder.getDataSourceName(), e);
	// 你的其他处理
}
// 你可以使用DataSourceContextHolder.getDataSourceName()来得到当前使用的数据源
// 你也可以使用SwitchableDataSource.getCurrentDataSOurceName()来获得当前使用的数据源（SwitchableDataSource需要自行注入）
```

# 注意事项
- radial内部一些ContextHolder使用了ThreadLocal，但是，考虑到在线程池中的可重用线程中的ThreadLocal可能出现数据穿越，因此，本项目还引用了
  阿里的 [transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local) 组件。请在使用radial时配置它，推荐使用javaagent方式配置。
- 若你的classpath中有radial依赖，且你的入口类上有@EnableSwitchableDataSource，radial才会启动并接替其他的DataSource创建过程，将SwitchableDataSource注入到你的spring容器里，
  SwitchableDataSource实现自DataSource，因此，在本工程今后的过程中，你所@Autowired的DataSource均是SwitchableDataSource。相反，若你不希望使用radial（多数据源），那么请不要引入radial依赖，或者不添加@EnableSwitchableDataSource到你的主类上。
- radial启动后会去读取配置以装配多数据源，所以，当你使用radial时，务必在spring配置中增加“**radial.datasources**”节点并写上你的详细配置。
- 默认情况下数据源会使用你配置中的**第一个未被停用的数据源**。为了避免不可预见的问题，我们建议在所有需要使用数据源的地方都使用"@UseDataSource"注解标注好你想要使用的数据源。
- 我们不建议配置很多数据源但是实际只使用了一部分的情况，若你确实不想删除配置，请将不用的数据源按照本文中的方法禁用。
- **仔细观察运行日志（INFO级别），它会告诉你很多有用的信息，包括DataSource注册过程、当前可用的数据源名称、详细的错误信息等等。**
- 目前radial支持各种数据源实现，但是我们推荐使用这两种池化数据源：hikiraCP（首选）和Druid。