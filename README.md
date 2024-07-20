# Polaris ORM

Polaris is Java ORM framework


![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![Apache](https://img.shields.io/badge/License-Apache_v2-blue.svg)](./LICENSE)
[![Coveralls](https://github.com/polaris-orm/polaris/actions/workflows/coveralls.yaml/badge.svg)](https://github.com/polaris-orm/polaris/actions/workflows/coveralls.yaml)
[![CI](https://github.com/polaris-orm/polaris/actions/workflows/multi-env.yaml/badge.svg)](https://github.com/polaris-orm/polaris/actions/workflows/multi-env.yaml)
[![Coverage Status](https://coveralls.io/repos/github/polaris-orm/polaris/badge.svg?branch=main)](https://coveralls.io/github/polaris-orm/polaris?branch=main)

**You ask me what the elegant code looks like? Then I have to show it!**

## 🛠️ 安装

### Gradle

```groovy
implementation 'cn.taketoday:polaris:0.0.1-SNAPSHOT'
```


## 示例

### RepositoryManager

```java
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

### EntityManager

### 
```java

@GET
public Pagination<ArticleItem> getArticlesByCategory(String categoryName, Pageable pageable) { 
  return entityManager.page(ArticleItem.class, Map.of("status", PostStatus.PUBLISHED, "category", categoryName), pageable)
        .peek(this::applyTags)
        .map(Pagination::from);
}

@GET
public Pagination<Article> search(ArticleConditionForm from, Pageable pageable) { 
  return entityManager.page(Article.class, from, pageable)
         .map(page -> Pagination.ok(page.getRows(), page.getTotalRows().intValue(), pageable));
}
  
@Getter
@Setter
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

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [TODAY Infrastructure](https://github.com/TAKETODAY/today-infrastructure): A Java library for applications software infrastructure.
* [Jetbrains](https://www.jetbrains.com/?from=https://github.com/polaris-orm/polaris): 感谢 Jetbrains 提供免费开源授权

## 📄 开源协议

使用 [Apache License](https://github.com/polaris-orm/polaris/blob/master/LICENSE)

