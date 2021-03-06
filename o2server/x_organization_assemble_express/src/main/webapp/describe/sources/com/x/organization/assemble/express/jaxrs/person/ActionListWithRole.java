package com.x.organization.assemble.express.jaxrs.person;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.tools.ListTools;
import com.x.organization.assemble.express.Business;
import com.x.organization.core.entity.Group;
import com.x.organization.core.entity.Group_;
import com.x.organization.core.entity.Role;

import net.sf.ehcache.Element;

class ActionListWithRole extends BaseAction {

	ActionResult<Wo> execute(EffectivePerson effectivePerson, JsonElement jsonElement) throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Wi wi = this.convertToWrapIn(jsonElement, Wi.class);
			ActionResult<Wo> result = new ActionResult<>();
			Business business = new Business(emc);
			String cacheKey = ApplicationCache.concreteCacheKey(this.getClass(),
					StringUtils.join(wi.getRoleList(), ","));
			Element element = cache.get(cacheKey);
			if (null != element && (null != element.getObjectValue())) {
				result.setData((Wo) element.getObjectValue());
			} else {
				Wo wo = this.list(business, wi);
				cache.put(new Element(cacheKey, wo));
				result.setData(wo);
			}
			return result;
		}
	}

	public static class Wi extends GsonPropertyObject {

		@FieldDescribe("角色")
		private List<String> roleList = new ArrayList<>();

		public List<String> getRoleList() {
			return roleList;
		}

		public void setRoleList(List<String> roleList) {
			this.roleList = roleList;
		}

	}

	public static class Wo extends WoPersonListAbstract {

	}

	private Wo list(Business business, Wi wi) throws Exception {
		List<Role> os = business.role().pick(wi.getRoleList());
		List<String> groupIds = new ArrayList<>();
		List<String> personIds = new ArrayList<>();
		for (Role o : os) {
			personIds.addAll(o.getPersonList());
			for (String groupId : o.getGroupList()) {
				groupIds.add(groupId);
				groupIds.addAll(business.group().listSubNested(groupId));
			}
		}
		groupIds = ListTools.trim(groupIds, true, true);
		personIds.addAll(this.listPersonWithGroup(business, groupIds));
		personIds = ListTools.trim(personIds, true, true);
		List<String> values = business.person().listPersonDistinguishedNameSorted(personIds);
		Wo wo = new Wo();
		wo.getPersonList().addAll(values);
		return wo;
	}

	private List<String> listPersonWithGroup(Business business, List<String> groupIds) throws Exception {
		EntityManager em = business.entityManagerContainer().get(Group.class);
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Group> cq = cb.createQuery(Group.class);
		Root<Group> root = cq.from(Group.class);
		Predicate p = root.get(Group_.id).in(groupIds);
		List<Group> os = em.createQuery(cq.select(root).where(p).distinct(true)).getResultList();
		List<String> personIds = new ArrayList<>();
		for (Group o : os) {
			personIds.addAll(o.getPersonList());
		}
		personIds = ListTools.trim(personIds, true, true);
		return personIds;
	}
}