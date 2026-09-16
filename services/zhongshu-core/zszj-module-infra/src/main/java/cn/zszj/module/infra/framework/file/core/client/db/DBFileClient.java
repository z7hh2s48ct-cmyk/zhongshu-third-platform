package cn.zszj.module.infra.framework.file.core.client.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.module.infra.dal.dataobject.file.FileContentDO;
import cn.zszj.module.infra.dal.mysql.file.FileContentMapper;
import cn.zszj.module.infra.framework.file.core.client.AbstractFileClient;

import java.util.Comparator;
import java.util.List;

/**
 * 基于 DB 存储的文件客户端的配置类
 *
 * @author 芋道源码
 */
public class DBFileClient extends AbstractFileClient<DBFileClientConfig> {

    private FileContentMapper fileContentMapper;

    public DBFileClient(Long id, DBFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
        fileContentMapper = SpringUtil.getBean(FileContentMapper.class);
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        FileContentDO contentDO = new FileContentDO().setConfigId(getId())
                .setPath(path).setContent(content);
        fileContentMapper.insert(contentDO);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public void delete(String path) {
        fileContentMapper.deleteByConfigIdAndPath(getId(), path);
    }

    @Override
    public byte[] getContent(String path) {
        List<FileContentDO> list = fileContentMapper.selectListByConfigIdAndPath(getId(), path);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        // 排序后，拿 id 最大的，即最后上传的
        list.sort(Comparator.comparing(FileContentDO::getId));
        return CollUtil.getLast(list).getContent();
    }

    /**
     * ZS-FILE-004.A（codex r1 P2）：范围读取——内容存于本库 FileContentDO，整读后内存裁切，
     * 不产生跨存储流量。
     */
    @Override
    public byte[] getContentRange(String path, long start, int length) {
        byte[] content = getContent(path);
        if (content == null) {
            return null;
        }
        int from = (int) Math.min(start, content.length);
        int to = (int) Math.min(start + length, content.length);
        return java.util.Arrays.copyOfRange(content, from, to);
    }

    /**
     * ZS-FILE-005.B：对象清点——按 config 分组 path（max(create_time) 作 lastModified 保留期锚点），
     * 不拉取 content 大字段（size 置 null，服务层对 null 保守跳过）。LIMIT 双方言（H2/PG）可移植。
     */
    @Override
    public java.util.List<cn.zszj.module.infra.framework.file.core.client.FileObjectEntry> listObjects(
            String prefix, int maxEntries) {
        return fileContentMapper.selectPathSummariesByPrefix(getId(), prefix, maxEntries).stream()
                .map(row -> new cn.zszj.module.infra.framework.file.core.client.FileObjectEntry(
                        row.getPath(), null, row.getLastModified()))
                .toList();
    }

}
