package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.Category;
import com.sky.blog.entity.Tag;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.mapper.ArticleTagMapper;
import com.sky.blog.mapper.CategoryMapper;
import com.sky.blog.mapper.TagMapper;
import com.sky.blog.security.Principal;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.SiteVo;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分类与标签（作业 1.4 的必做项之一）。
 *
 * <p>两者规则几乎一样，放在一起少一层间接：都是"名字唯一 + 删除前要确认没有已发布文章在用"。
 */
@Service
public class TaxonomyService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final AuditLogService auditLogService;

    public TaxonomyService(CategoryMapper categoryMapper, TagMapper tagMapper,
                           ArticleMapper articleMapper, ArticleTagMapper articleTagMapper,
                           AuditLogService auditLogService) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.auditLogService = auditLogService;
    }

    // ------------------------------------------------------------ 查询（公开）

    public List<SiteVo.Category> categoriesWithCount() {
        return categoryMapper.selectAllWithCount().stream()
                .map(c -> new SiteVo.Category(c.getId(), c.getName(), c.getSortOrder(),
                        c.getArticleCount() == null ? 0L : c.getArticleCount()))
                .toList();
    }

    public List<SiteVo.Tag> tagsWithCount() {
        return tagMapper.selectAllWithCount().stream()
                .map(t -> new SiteVo.Tag(t.getId(), t.getName(),
                        t.getArticleCount() == null ? 0L : t.getArticleCount()))
                .toList();
    }

    /** 供文章列表/详情批量取标签名，避免 N+1 */
    public Map<Long, List<String>> tagNamesByArticleIds(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<com.sky.blog.entity.ArticleTag> links = articleTagMapper.selectByArticleIds(articleIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = links.stream().map(com.sky.blog.entity.ArticleTag::getTagId).distinct().toList();
        Map<Long, String> idToName = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return links.stream().collect(Collectors.groupingBy(
                com.sky.blog.entity.ArticleTag::getArticleId,
                Collectors.mapping(link -> idToName.get(link.getTagId()),
                        Collectors.filtering(java.util.Objects::nonNull, Collectors.toList()))));
    }

    public Map<Long, Category> categoriesByIds(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    /** 校验分类/标签 id 是否都存在，避免文章挂到不存在的分类上 */
    public void assertIdsExist(Long categoryId, List<Long> tagIds) {
        if (categoryId != null && categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类不存在：" + categoryId);
        }
        List<Long> distinct = tagIds == null ? List.of()
                : tagIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return;
        }
        if (distinct.size() > 10) {
            throw BizException.unprocessable("一篇文章最多 10 个标签");
        }
        long found = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().in(Tag::getId, distinct));
        if (found != distinct.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在无效的标签 id");
        }
    }

    // ------------------------------------------------------------ 分类管理（后台）

    @Transactional(rollbackFor = Exception.class)
    public SiteVo.Category createCategory(Principal op, AdminDto.CategorySave dto) {
        String name = dto.name().trim();
        if (categoryMapper.exists(new LambdaQueryWrapper<Category>().eq(Category::getName, name))) {
            throw new BizException(ErrorCode.DUPLICATE, "分类名已存在");
        }
        Category row = new Category();
        row.setName(name);
        row.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());
        categoryMapper.insert(row);
        auditLogService.record(op, AuditAction.CATEGORY_CREATE, AuditTarget.CATEGORY, row.getId(), name, null);
        return new SiteVo.Category(row.getId(), row.getName(), row.getSortOrder(), 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public SiteVo.Category updateCategory(Principal op, Long id, AdminDto.CategorySave dto) {
        Category row = mustCategory(id);
        String name = dto.name().trim();
        boolean duplicated = categoryMapper.exists(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name).ne(Category::getId, id));
        if (duplicated) {
            throw new BizException(ErrorCode.DUPLICATE, "分类名已存在");
        }
        String before = row.getName();
        row.setName(name);
        if (dto.sortOrder() != null) {
            row.setSortOrder(dto.sortOrder());
        }
        categoryMapper.updateById(row);
        auditLogService.record(op, AuditAction.CATEGORY_UPDATE, AuditTarget.CATEGORY, id, name,
                "名称 " + before + " -> " + name);
        return new SiteVo.Category(row.getId(), row.getName(), row.getSortOrder(), 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Principal op, Long id) {
        Category row = mustCategory(id);
        long inUse = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, id)
                .eq(Article::getStatus, ArticleStatus.PUBLISHED));
        if (inUse > 0) {
            throw BizException.unprocessable("该分类下还有 " + inUse + " 篇已发布文章，请先移动或删除");
        }
        // 没有已发布文章在用：把草稿/待审等其他文章的分类置空，再删除分类本身
        articleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
                .eq(Article::getCategoryId, id)
                .set(Article::getCategoryId, null));
        categoryMapper.deleteById(id);
        auditLogService.record(op, AuditAction.CATEGORY_DELETE, AuditTarget.CATEGORY, id, row.getName(), null);
    }

    // ------------------------------------------------------------ 标签管理（后台）

    @Transactional(rollbackFor = Exception.class)
    public SiteVo.Tag createTag(Principal op, AdminDto.TagSave dto) {
        String name = dto.name().trim();
        if (tagMapper.exists(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name))) {
            throw new BizException(ErrorCode.DUPLICATE, "标签名已存在");
        }
        Tag row = new Tag();
        row.setName(name);
        tagMapper.insert(row);
        auditLogService.record(op, AuditAction.TAG_CREATE, AuditTarget.TAG, row.getId(), name, null);
        return new SiteVo.Tag(row.getId(), row.getName(), 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public SiteVo.Tag updateTag(Principal op, Long id, AdminDto.TagSave dto) {
        Tag row = mustTag(id);
        String name = dto.name().trim();
        if (tagMapper.exists(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name).ne(Tag::getId, id))) {
            throw new BizException(ErrorCode.DUPLICATE, "标签名已存在");
        }
        String before = row.getName();
        row.setName(name);
        tagMapper.updateById(row);
        auditLogService.record(op, AuditAction.TAG_UPDATE, AuditTarget.TAG, id, name,
                "名称 " + before + " -> " + name);
        return new SiteVo.Tag(row.getId(), row.getName(), 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Principal op, Long id) {
        Tag row = mustTag(id);
        long linked = articleTagMapper.countByTag(id);
        if (linked > 0) {
            throw BizException.unprocessable("该标签还被 " + linked + " 篇文章使用，请先解除关联");
        }
        tagMapper.deleteById(id);
        auditLogService.record(op, AuditAction.TAG_DELETE, AuditTarget.TAG, id, row.getName(), null);
    }

    // ------------------------------------------------------------ 校验

    public Category mustCategory(Long id) {
        Category row = id == null ? null : categoryMapper.selectById(id);
        if (row == null) {
            throw BizException.notFound();
        }
        return row;
    }

    public Tag mustTag(Long id) {
        Tag row = id == null ? null : tagMapper.selectById(id);
        if (row == null) {
            throw BizException.notFound();
        }
        return row;
    }

    /** 后台列表：分类带计数 */
    public PageVo<SiteVo.Category> adminCategoryPage(long current, long size) {
        List<SiteVo.Category> all = categoriesWithCount();
        int from = (int) Math.min((current - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return new PageVo<>(all.subList(from, to), all.size(), current, size,
                (all.size() + size - 1) / size);
    }
}
