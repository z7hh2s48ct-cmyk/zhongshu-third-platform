package cn.zszj.module.infra.dal.mysql.file;

import cn.zszj.module.infra.dal.dataobject.file.FileContentDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileContentMapper extends BaseMapper<FileContentDO> {

    default void deleteByConfigIdAndPath(Long configId, String path) {
        this.delete(new LambdaQueryWrapper<FileContentDO>()
                .eq(FileContentDO::getConfigId, configId)
                .eq(FileContentDO::getPath, path));
    }

    default List<FileContentDO> selectListByConfigIdAndPath(Long configId, String path) {
        return selectList(new LambdaQueryWrapper<FileContentDO>()
                .eq(FileContentDO::getConfigId, configId)
                .eq(FileContentDO::getPath, path));
    }

    /**
     * 对象清点摘要（ZS-FILE-005.B 孤儿预览）：按 path 分组取 max(create_time) 作保留期锚点，
     * 不读取 content 大字段。LIMIT 双方言（H2/PG）可移植。
     */
    @Select("SELECT path AS path, MAX(create_time) AS lastModified FROM infra_file_content "
            + "WHERE config_id = #{configId} AND deleted = 0 AND path LIKE CONCAT(#{prefix}, '%') "
            + "GROUP BY path ORDER BY path LIMIT #{limit}")
    List<PathSummary> selectPathSummariesByPrefix(@Param("configId") Long configId,
                                                  @Param("prefix") String prefix,
                                                  @Param("limit") int limit);

    /**
     * 清点摘要投影（MyBatis 按属性名装配）
     */
    class PathSummary {

        private String path;
        private LocalDateTime lastModified;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public LocalDateTime getLastModified() {
            return lastModified;
        }

        public void setLastModified(LocalDateTime lastModified) {
            this.lastModified = lastModified;
        }
    }

}
