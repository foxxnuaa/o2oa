package com.x.organization.assemble.control.jaxrs.personcard;

import com.x.base.core.project.exception.PromptException;
import com.x.base.core.project.http.EffectivePerson;

class ExceptionDenyCreateRole extends PromptException {

	private static final long serialVersionUID = 4132300948670472899L;

	ExceptionDenyCreateRole(EffectivePerson effectivePerson, String name) {
		super("{} 不能创建角色:{}, 权限不足.", effectivePerson.getDistinguishedName(), name);
	}
}
