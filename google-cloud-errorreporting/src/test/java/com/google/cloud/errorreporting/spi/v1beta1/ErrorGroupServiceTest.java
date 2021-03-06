/*
 * Copyright 2016, Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.errorreporting.spi.v1beta1;

import com.google.api.gax.grpc.ApiException;
import com.google.api.gax.testing.MockGrpcService;
import com.google.api.gax.testing.MockServiceHelper;
import com.google.devtools.clouderrorreporting.v1beta1.ErrorGroup;
import com.google.devtools.clouderrorreporting.v1beta1.GetGroupRequest;
import com.google.devtools.clouderrorreporting.v1beta1.UpdateGroupRequest;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@javax.annotation.Generated("by GAPIC")
public class ErrorGroupServiceTest {
  private static MockErrorGroupService mockErrorGroupService;
  private static MockErrorStatsService mockErrorStatsService;
  private static MockReportErrorsService mockReportErrorsService;
  private static MockServiceHelper serviceHelper;
  private ErrorGroupServiceClient client;

  @BeforeClass
  public static void startStaticServer() {
    mockErrorGroupService = new MockErrorGroupService();
    mockErrorStatsService = new MockErrorStatsService();
    mockReportErrorsService = new MockReportErrorsService();
    serviceHelper =
        new MockServiceHelper(
            "in-process-1",
            Arrays.<MockGrpcService>asList(
                mockErrorGroupService, mockErrorStatsService, mockReportErrorsService));
    serviceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    serviceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    serviceHelper.reset();
    ErrorGroupServiceSettings settings =
        ErrorGroupServiceSettings.defaultBuilder()
            .setChannelProvider(serviceHelper.createChannelProvider())
            .build();
    client = ErrorGroupServiceClient.create(settings);
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  @Test
  @SuppressWarnings("all")
  public void getGroupTest() {
    String name = "name3373707";
    String groupId = "groupId506361563";
    ErrorGroup expectedResponse = ErrorGroup.newBuilder().setName(name).setGroupId(groupId).build();
    mockErrorGroupService.addResponse(expectedResponse);

    String formattedGroupName = ErrorGroupServiceClient.formatGroupName("[PROJECT]", "[GROUP]");

    ErrorGroup actualResponse = client.getGroup(formattedGroupName);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<GeneratedMessageV3> actualRequests = mockErrorGroupService.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetGroupRequest actualRequest = (GetGroupRequest) actualRequests.get(0);

    Assert.assertEquals(formattedGroupName, actualRequest.getGroupName());
  }

  @Test
  @SuppressWarnings("all")
  public void getGroupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    mockErrorGroupService.addException(exception);

    try {
      String formattedGroupName = ErrorGroupServiceClient.formatGroupName("[PROJECT]", "[GROUP]");

      client.getGroup(formattedGroupName);
      Assert.fail("No exception raised");
    } catch (ApiException e) {
      Assert.assertEquals(Status.INTERNAL.getCode(), e.getStatusCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void updateGroupTest() {
    String name = "name3373707";
    String groupId = "groupId506361563";
    ErrorGroup expectedResponse = ErrorGroup.newBuilder().setName(name).setGroupId(groupId).build();
    mockErrorGroupService.addResponse(expectedResponse);

    ErrorGroup group = ErrorGroup.newBuilder().build();

    ErrorGroup actualResponse = client.updateGroup(group);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<GeneratedMessageV3> actualRequests = mockErrorGroupService.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateGroupRequest actualRequest = (UpdateGroupRequest) actualRequests.get(0);

    Assert.assertEquals(group, actualRequest.getGroup());
  }

  @Test
  @SuppressWarnings("all")
  public void updateGroupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    mockErrorGroupService.addException(exception);

    try {
      ErrorGroup group = ErrorGroup.newBuilder().build();

      client.updateGroup(group);
      Assert.fail("No exception raised");
    } catch (ApiException e) {
      Assert.assertEquals(Status.INTERNAL.getCode(), e.getStatusCode());
    }
  }
}
