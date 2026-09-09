# Polaris ORM

Polaris 是一个 Java ORM 框架

![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![Apache](https://img.shields.io/badge/License-Apache_v2-blue.svg)](./LICENSE)
[![Coveralls](https://github.com/polaris-orm/polaris-orm/actions/workflows/coveralls.yaml/badge.svg)](https://github.com/polaris-orm/polaris-orm/actions/workflows/coveralls.yaml)
[![CI](https://github.com/polaris-orm/polaris-orm/actions/workflows/multi-env.yaml/badge.svg)](https://github.com/polaris-orm/polaris-orm/actions/workflows/multi-env.yaml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e482b7d1bd0d474da5a0cdcbd42fe135)](https://app.codacy.com/gh/polaris-orm/polaris-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Coverage Status](https://coveralls.io/repos/github/polaris-orm/polaris-orm/badge.svg?branch=main)](https://coveralls.io/github/polaris-orm/polaris-orm?branch=main)


本项目是 [TAKETODAY/today-infrastructure/infra-jdbc](https://github.com/TAKETODAY/today-infrastructure/tree/master/infra-jdbc) 的一个分支。**欢迎大家贡献代码**

## 特点

- 轻量，不依赖第三方库。
- 更好的性能，用字节码技术避免反射调用，JDBC 之上的一层很薄的封装。
- 自动生成 添加、删除、修改和简单查询数据库的 SQL。
- 支持多种数据库，通过 Platform 接口灵活扩展。
- 优雅的 API 设计。代码优雅，易维护。

## 示例

### Model
```java
@Table("article")
public class Article {
    
  @Id
  public Long id;

  public String cover;

  @Column("title")
  public String title;

  public Instant createAt;

// ...
}
```

### 使用 EntityManager 

### 
```java

@POST("/articles")
@ResponseStatus(HttpStatus.CREATED)
@Logging(title = "创建文章", content = "标题: [#{#form.title}]")
public void create(@RequestBody ArticleForm form) { 
  Article article = Article.forCreation(form);
  entityManager.persist(article);
}

@PUT("/articles/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@Logging(title = "更新文章", content = "更新文章: [#{#from.title}]")
public void update(@PathVariable("id") Long id, @RequestBody ArticleForm from) {
  Article article = Article.forUpdate(from);
  article.setId(id);
  article.setUpdateAt(Instant.now());
//  entityManager.updateById(article);
  entityManager.updateById(article, PropertyUpdateStrategy.noneNull());
}

@DELETE("/articles/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@Logging(title = "删除文章", content = "删除文章: [#{#id}]")
public void delete(@PathVariable Long id) {
  entityManager.deleteById(id);
}

@GET
public Pagination<ArticleItem> getArticlesByCategory(String categoryName, Pageable pageable) { 
  return entityManager.page(ArticleItem.class, Map.of("status", PostStatus.PUBLISHED, "category", categoryName), pageable)
        .peek(this::applyTags)
        .map(Pagination::from);
}

// 复杂查询

@GET
public Pagination<Article> search(ArticleConditionForm from, Pageable pageable) { 
  return entityManager.page(Article.class, from, pageable)
         .map(page -> Pagination.ok(page.getRows(), page.getTotalRows().intValue(), pageable));
}
  
// 复杂表单
@Data
public class ArticleConditionForm implements ConditionStatement, DebugDescriptive {

  @Nullable
  private String q;

  @Nullable
  private String category;

  @Nullable
  private PostStatus status;

  @Nullable
  private Map<String, OrderBy> sort;

  @Nullable
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime[] createAt;

  @Nullable
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime[] updateAt;

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    if (StringUtils.hasText(q)) {
      restrictions.add(Restriction.plain(" (`title` like ? OR `content` like ? )"));
    }

    if (StringUtils.hasText(category)) {
      restrictions.add(Restriction.equal("category"));
    }

    if (status != null) {
      restrictions.add(Restriction.equal("status"));
    }

    if (createAt != null && createAt.length == 2) {
      restrictions.add(Restriction.plain("create_at between ? and ?"));
    }

    if (updateAt != null && updateAt.length == 2) {
      restrictions.add(Restriction.plain("update_at between ? and ?"));
    }

  }

  @Nullable
  @Override
  public OrderByClause getOrderByClause(EntityMetadata metadata) {
    if (sort != null) {
      List<Pair<String, Order>> list = sort.entrySet().stream()
              .map(entry -> {
                EntityProperty property = metadata.findProperty(entry.getKey());
                if (property != null) {
                  return Pair.of(property.columnName, entry.getValue().order);
                }
                return null;
              })
              .filter(Objects::nonNull)
              .toList();
      return new MutableOrderByClause(list);
    }
    return OrderByClause.plain("update_at DESC, create_at DESC");
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement smt) throws SQLException {
    int idx = 1;
    if (StringUtils.hasText(q)) {
      String string = '%' + q.trim() + '%';
      smt.setString(idx++, string);
      smt.setString(idx++, string);
    }

    if (StringUtils.hasText(category)) {
      smt.setString(idx++, category.trim());
    }

    if (status != null) {
      smt.setInt(idx++, status.getValue());
    }

    if (createAt != null && createAt.length == 2) {
      smt.setObject(idx++, createAt[0]);
      smt.setObject(idx++, createAt[1]);
    }

    if (updateAt != null && updateAt.length == 2) {
      smt.setObject(idx++, updateAt[0]);
      smt.setObject(idx, updateAt[1]);
    }

  }

  @Override
  public String getDescription() {
    return "Articles searching";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Articles searching with [{}]", this);
  }

}

```


### 使用 RepositoryManager 

> 这种方式提供了接近原生 JDBC 的性能。

```java

@Nullable
@Cacheable(key = "'getByURI_'+#uri")
public Article getByURI(String uri) {
  Assert.notNull(uri, "文章地址不能为空");
  try (Query query = repository.createQuery("SELECT * FROM article WHERE uri=? LIMIT 1")) {
    query.addParameter(uri);
    
    Article article = query.fetchFirst(Article.class);
    applyTags(article);
    return article; 
  }
}

public Pagination<ArticleItem> getHomeArticles(Pageable pageable) {
  try (JdbcConnection connection = repository.open()) {
    try (Query countQuery = connection.createQuery(
            "SELECT COUNT(id) FROM article WHERE `status` = ?")) {
      countQuery.addParameter(PostStatus.PUBLISHED);
      int count = countQuery.fetchScalar(int.class);
      if (count < 1) {
        return Pagination.empty();
      }

      String sql = """
              SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
              FROM article WHERE `status` = :status
              order by create_at DESC LIMIT :offset, :pageSize
              """;
      try (NamedQuery query = repository.createNamedQuery(sql)) {
        query.addParameter("offset", pageable.offset());
        query.addParameter("status", PostStatus.PUBLISHED);
        query.addParameter("pageSize", pageable.pageSize());

        return fetchArticleItems(pageable, count, query);
      }
    }
  }
}
```

## 🛠️ 安装

暂时还未发布到 Maven 中央仓库

### Gradle
```groovy
implementation 'cn.taketoday:polaris-orm:0.0.1-SNAPSHOT'
```


## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [TODAY Infrastructure](https://github.com/TAKETODAY/today-infrastructure): A Java library for applications software infrastructure.
* [Jetbrains](https://www.jetbrains.com/?from=https://github.com/polaris-orm/polaris): 感谢 Jetbrains 提供免费开源授权

## 📄 开源协议

使用 [Apache License](https://github.com/polaris-orm/polaris/blob/master/LICENSE)

