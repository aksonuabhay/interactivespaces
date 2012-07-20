<#--
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 -->
<!DOCTYPE html>
<html>
<head>
<title>Interactive Spaces Admin: Spaces</title>

<#include "/allpages_head.ftl">
</head>

<body>

<#include "/allpages_body_header.ftl">

<h1>Live Activities for Space: ${space.name}</h1>

<p><a href="view.html">Back to main space page</a></p>

<table>
<tr><th>Live Activity</th><th>Status</th><th>Up to date?</th></td>
<#list liveactivitygroups as liveactivitygroup>
<tr style="background-color: #e0e0e0; font-weight: bold"><td colspan="3">Live Activity Group: <a href="/interactivespaces/liveactivitygroup/${liveactivitygroup.liveActivityGroup.id}/view.html">${liveactivitygroup.liveActivityGroup.name}</a></td></tr>
<#list liveactivitygroup.liveActivities as liveactivity>
<#assign trCss = (liveactivity_index % 2 == 0)?string("even","odd")>
<tr class="${trCss}">
<td><a href="/interactivespaces/liveactivity/${liveactivity.activity.id}/view.html">${liveactivity.activity.name}</a></td>
<td><#if liveactivity.active?has_content>
<@spring.message liveactivity.active.state.description />
 as of 
  <#if liveactivity.active.lastStateUpdate??>
    ${liveactivity.active.lastStateUpdateDate?datetime}
  <#else>
    Unknown
  </#if>
<#else>
<span style="color: red;">No controller assigned!</span>
</#if>
</td>
<td>
<#if liveactivity.activity.outOfDate>
<span title="Live Activity is out of date"><img src="/interactivespaces/img/outofdate.png" alt="Live Activity is out of date" /></span>
</#if>
</td>
    </tr>
<tr class="${trCss}">
<td>&nbsp;</td>
<td>
<#if liveactivity.active.directRunning>
Directly Running
<#else>
Not Directly Running
</#if> 
&bull;
<#if liveactivity.active.directActivated>
Directly Activated
<#else>
Not Directly Activated
</#if> 
&bull;
running from ${liveactivity.active.numberLiveActivityGroupRunning} groups
&bull;
activated from ${liveactivity.active.numberLiveActivityGroupActivated} groups
</td>
<td>&nbsp;</td>
</tr>
</#list>
</#list>
</table>

</body>
<html>