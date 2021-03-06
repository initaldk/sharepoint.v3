// Copyright 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.sharepoint.social;

import com.google.common.collect.Sets;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.SimpleProperty;
import com.google.enterprise.connector.spi.SocialUserProfileDocument;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import com.google.enterprise.connector.spi.SpiConstants.CaseSensitivityType;
import com.google.enterprise.connector.spi.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SharePointSocialUserProfileDocument 
    extends SocialUserProfileDocument {
  
  private int nextValue = -1; 

  // List of groups to be sent in document's ACL.
  private Set<Principal> aclGroups;

  private ActionType actionType;

  public SharePointSocialUserProfileDocument(String collectionName) {
    super(collectionName);    
  }  

  public int getNextValue() {
    return nextValue;
  }

  public void setNextValue(int nextValue) {
    this.nextValue = nextValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Property findProperty(String name) {
    if (SpiConstants.PROPNAME_ACLGROUPS.equalsIgnoreCase(name)) {
      if (aclGroups != null) {
        List<Value> values = new ArrayList<Value>(aclGroups.size());
        for (Principal group : aclGroups) {
          values.add(Value.getPrincipalValue(group));
        }
        return new SimpleProperty(values);
      } else {
        return null;
      }
    }
    return super.findProperty(name);
  }
  
  public void addAclGroupToDocument(String globalNamespace,
      String principalName) {
   if (aclGroups == null) {
     aclGroups = Sets.newHashSet();
   }
   aclGroups.add(new Principal(SpiConstants.PrincipalType.UNKNOWN,
       globalNamespace, principalName,
       CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE));
  }

  public ActionType getActionType() {
    return actionType;
  }

  public void setActionType(ActionType actionType) {
    this.actionType = actionType;
  }
}
