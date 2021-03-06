/**
 * Copyright Dingxuan. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bcia.javachain.common.groupconfig.capability;

import org.bcia.javachain.common.groupconfig.GroupConfigConstant;
import org.bcia.julongchain.protos.common.Configuration;

import java.util.Map;

/**
 * 应用提供者
 *
 * @author zhouhui
 * @date 2018/5/9
 * @company Dingxuan
 */
public class ApplicationProvider implements IApplicationCapabilities {
    private Map<String, Configuration.Capability> capabilityMap;

    private boolean supported;
    private boolean forbidDuplicateTxId;
    private boolean resourcesTree;
    private boolean privateGroupData;
    private boolean validation;

    public ApplicationProvider(Map<String, Configuration.Capability> capabilityMap) {
        this.capabilityMap = capabilityMap;

        this.supported = true;
        this.forbidDuplicateTxId = capabilityMap.containsKey(GroupConfigConstant.APP_FORBID_DUPLICATE_TXID);
        this.resourcesTree = capabilityMap.containsKey(GroupConfigConstant.APP_RESOURCE_TREE_EXPERIMENTAL);
        this.privateGroupData = capabilityMap.containsKey(GroupConfigConstant.APP_PRIVATE_DATA_EXPERIMENTAL);
        this.validation = capabilityMap.containsKey(GroupConfigConstant.APP_VALIDATION);
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean isForbidDuplicateTxId() {
        return forbidDuplicateTxId;
    }

    @Override
    public boolean isResourcesTree() {
        return resourcesTree;
    }

    @Override
    public boolean isPrivateGroupData() {
        return privateGroupData;
    }

    @Override
    public boolean isValidation() {
        return validation;
    }
}
