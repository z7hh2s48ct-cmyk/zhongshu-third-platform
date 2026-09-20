package cn.zszj.module.system.service.organization;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OrganizationServiceImpl} 的单元测试类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-002）：组织类型/层级/编码唯一/删除守卫。
 *
 * @author ZS-IAM-002
 */
@Import(OrganizationServiceImpl.class)
public class OrganizationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OrganizationServiceImpl organizationService;
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private MembershipMapper membershipMapper;

    private OrganizationDO randomOrganizationDO() {
        return randomPojo(OrganizationDO.class, o -> {
            o.setType(randomEle(OrganizationTypeEnum.values()).getType());
            o.setParentId(OrganizationDO.PARENT_ID_ROOT);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
    }

    @Test
    public void testCreateOrganization_success() {
        OrganizationDO organization = randomOrganizationDO();
        organization.setId(null);

        Long id = organizationService.createOrganization(organization);

        assertNotNull(id);
        OrganizationDO db = organizationMapper.selectById(id);
        assertNotNull(db);
        assertEquals(organization.getName(), db.getName());
        assertEquals(organization.getType(), db.getType());
    }

    @Test
    public void testCreateOrganization_codeDuplicate() {
        OrganizationDO existing = randomOrganizationDO();
        organizationMapper.insert(existing);
        OrganizationDO dup = randomOrganizationDO();
        dup.setId(null);
        dup.setCode(existing.getCode());

        assertServiceException(() -> organizationService.createOrganization(dup),
                ORGANIZATION_CODE_DUPLICATE, existing.getCode());
    }

    @Test
    public void testCreateOrganization_typeInvalid() {
        OrganizationDO organization = randomOrganizationDO();
        organization.setId(null);
        organization.setType(999);

        assertServiceException(() -> organizationService.createOrganization(organization),
                ORGANIZATION_TYPE_INVALID, 999);
    }

    @Test
    public void testUpdateOrganization_parentNotExists() {
        OrganizationDO organization = randomOrganizationDO();
        organizationMapper.insert(organization);
        organization.setParentId(randomLongId());

        assertServiceException(() -> organizationService.updateOrganization(organization),
                ORGANIZATION_PARENT_NOT_EXISTS);
    }

    @Test
    public void testUpdateOrganization_parentSelfError() {
        OrganizationDO organization = randomOrganizationDO();
        organizationMapper.insert(organization);
        organization.setParentId(organization.getId());

        assertServiceException(() -> organizationService.updateOrganization(organization),
                ORGANIZATION_PARENT_ERROR);
    }

    @Test
    public void testUpdateOrganization_parentCycleError() {
        // parent -> child；将 parent 的父设为 child 形成环
        OrganizationDO parent = randomOrganizationDO();
        organizationMapper.insert(parent);
        OrganizationDO child = randomOrganizationDO();
        child.setParentId(parent.getId());
        organizationMapper.insert(child);
        // 将 parent 挂到 child 之下 → 祖先链自 child 上溯遇 parent.id → 成环
        parent.setParentId(child.getId());

        assertServiceException(() -> organizationService.updateOrganization(parent),
                ORGANIZATION_PARENT_ERROR);
    }

    @Test
    public void testDeleteOrganization_hasChildren() {
        OrganizationDO parent = randomOrganizationDO();
        organizationMapper.insert(parent);
        OrganizationDO child = randomOrganizationDO();
        child.setParentId(parent.getId());
        organizationMapper.insert(child);

        assertServiceException(() -> organizationService.deleteOrganization(parent.getId()),
                ORGANIZATION_HAS_CHILDREN);
        assertNotNull(organizationMapper.selectById(parent.getId()));
    }

    @Test
    public void testDeleteOrganization_hasMemberships() {
        OrganizationDO organization = randomOrganizationDO();
        organizationMapper.insert(organization);
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setOrganizationId(organization.getId());
            o.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
            o.setIsPrimary(1);
        });
        membershipMapper.insert(membership);

        assertServiceException(() -> organizationService.deleteOrganization(organization.getId()),
                ORGANIZATION_HAS_MEMBERSHIPS);
        assertNotNull(organizationMapper.selectById(organization.getId()));
    }

    @Test
    public void testDeleteOrganization_success() {
        OrganizationDO organization = randomOrganizationDO();
        organizationMapper.insert(organization);

        organizationService.deleteOrganization(organization.getId());
        assertNull(organizationMapper.selectById(organization.getId()));
    }

    @Test
    public void testValidateOrganizationEnabled_notExists() {
        assertServiceException(() -> organizationService.validateOrganizationEnabled(randomLongId()),
                ORGANIZATION_NOT_EXISTS);
    }

    @Test
    public void testValidateOrganizationEnabled_disabled() {
        OrganizationDO organization = randomOrganizationDO();
        organization.setStatus(CommonStatusEnum.DISABLE.getStatus());
        organizationMapper.insert(organization);

        assertServiceException(() -> organizationService.validateOrganizationEnabled(organization.getId()),
                ORGANIZATION_NOT_ENABLE, organization.getName());
    }

    @Test
    public void testGetOrganizationListByType() {
        OrganizationDO brand = randomOrganizationDO();
        brand.setType(OrganizationTypeEnum.BRAND.getType());
        organizationMapper.insert(brand);
        OrganizationDO store = randomOrganizationDO();
        store.setType(OrganizationTypeEnum.STORE.getType());
        organizationMapper.insert(store);

        assertEquals(1, organizationService.getOrganizationListByType(OrganizationTypeEnum.BRAND.getType()).size());
        assertEquals(brand.getId(),
                organizationService.getOrganizationListByType(OrganizationTypeEnum.BRAND.getType()).get(0).getId());
    }

}
