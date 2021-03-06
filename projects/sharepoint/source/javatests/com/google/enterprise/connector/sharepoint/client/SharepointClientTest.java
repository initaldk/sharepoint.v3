// Copyright 2007 Google Inc.
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

package com.google.enterprise.connector.sharepoint.client;

import com.google.common.collect.ImmutableList;
import com.google.enterprise.connector.sharepoint.TestConfiguration;
import com.google.enterprise.connector.sharepoint.client.SPConstants.FeedType;
import com.google.enterprise.connector.sharepoint.spiimpl.SPDocument;
import com.google.enterprise.connector.sharepoint.spiimpl.SPDocumentList;
import com.google.enterprise.connector.sharepoint.spiimpl.SharepointException;
import com.google.enterprise.connector.sharepoint.state.GlobalState;
import com.google.enterprise.connector.sharepoint.state.ListState;
import com.google.enterprise.connector.sharepoint.state.WebState;
import com.google.enterprise.connector.sharepoint.wsclient.mock.MockClientFactory;
import com.google.enterprise.connector.sharepoint.wsclient.soap.SPClientFactory;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethodBase;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class SharepointClientTest extends TestCase {

  private SharepointClient sharepointClient;
  private GlobalState globalState;
  private final SPClientFactory clientFactory = new SPClientFactory();

  protected void setUp() throws Exception {
    super.setUp();
    System.out.println("Initializing SharepointClientContext ...");
    final SharepointClientContext sharepointClientContext = TestConfiguration.initContext();
    assertNotNull(sharepointClientContext);
    sharepointClientContext.setIncluded_metadata(TestConfiguration.whiteList);
    sharepointClientContext.setExcluded_metadata(TestConfiguration.blackList);

    this.sharepointClient = new SharepointClient(clientFactory,
        sharepointClientContext);
    this.globalState = new GlobalState(clientFactory,
        sharepointClientContext.getGoogleConnectorWorkDir(),
        sharepointClientContext.getFeedType());
  }

  public void testUpdateGlbalState() throws SharepointException {
    System.out.println("Testing updateGlobalState()...");
    this.sharepointClient.updateGlobalState(this.globalState);
    assertNotNull(this.globalState);
    System.out.println("[ updateGlobalState() ] Test Completed.");
  }

  /**
   * Test method for
   * {@link com.google.enterprise.connector.sharepoint.client. SharepointClient#getDocsFromDocumentLibrary()}
   * .
   */
  public void testTraverse() throws SharepointException {
    System.out.println("Testing [ testTraverse() ]...");
    final int iPageSizeHint = 100;
    this.sharepointClient.updateGlobalState(this.globalState);
    Set<WebState> webStates = this.globalState.getAllWebStateSet();
    WebState curr_webState = webStates.iterator().next();
    SPDocumentList rs = this.sharepointClient.traverse(this.globalState,
        curr_webState, iPageSizeHint, false);
    int numDocs = 0;
    try {
      System.out.println("Documents found - ");
      if (rs != null) {
        SPDocument pm = (SPDocument) rs.nextDocument();
        while (pm != null) {
          System.out.println("<document>");
          final Property lastModProp = pm.findProperty(SpiConstants.PROPNAME_LASTMODIFIED);
          if (lastModProp != null) {
            System.out.println("<lastModify>"
                + lastModProp.nextValue().toString() + "</lastModify>");
          }
          final Property docProp = pm.findProperty(SpiConstants.PROPNAME_DOCID);
          if (lastModProp != null) {
            System.out.println("<docId>" + docProp.nextValue().toString()
                + "</docId>");
          }
          final Property searchUrlProp = pm.findProperty(SpiConstants.PROPNAME_SEARCHURL);
          if (searchUrlProp != null) {
            System.out.println("<searchUrl>"
                + searchUrlProp.nextValue().toString() + "</searchUrl>");
          }
          final Property listGuidProp = pm.findProperty(SPConstants.LIST_GUID);
          if (listGuidProp != null) {
            System.out.println("<listGuid>"
                + listGuidProp.nextValue().toString() + "</listguid>");
          }
          final Property displayUrlProp = pm.findProperty(SpiConstants.PROPNAME_DISPLAYURL);
          if (displayUrlProp != null) {
            System.out.println("<displayUrl>"
                + displayUrlProp.nextValue().toString() + "</displayUrl>");
          }
          final Property authorProp = pm.findProperty(SPConstants.AUTHOR);
          if (authorProp != null) {
            System.out.println("<author>" + authorProp.nextValue().toString()
                + "</author>");
          }
          final Property objTypeProp = pm.findProperty(SPConstants.OBJECT_TYPE);
          if (objTypeProp != null) {
            System.out.println("<" + SPConstants.OBJECT_TYPE + ">"
                + objTypeProp.nextValue().toString() + "<"
                + SPConstants.OBJECT_TYPE + ">");
          }
          final Property isPublicProp = pm.findProperty(SpiConstants.PROPNAME_ISPUBLIC);
          if (isPublicProp != null) {
            System.out.println("<isPublic>"
                + isPublicProp.nextValue().toString() + "</isPublic>");
          }
          System.out.println("</document>");

          pm.dumpAllAttrs();

          // check crawling coverage.. check if paticular document is
          // found
          numDocs++;

          pm = (SPDocument) rs.nextDocument();
        }
      }
    } catch (final RepositoryException e) {
      e.printStackTrace();
    }
    System.out.println("Total dos: " + numDocs);
    System.out.println("[ testTraverse() ] Test Completed.");
  }

  /**
   * Tests {@link SharepointClient#traverse(GlobalState, WebState, int)} to
   * check that only the lists starting from the given list are checked for
   * pending docs from previous crawl cycle
   *
   * @throws SharepointException
   */
  public void testTraverseToCheckValidLists() throws SharepointException {
    SharepointClientContext spContext = TestConfiguration.initContext();
    spContext.setBatchHint(Integer.MAX_VALUE);
    GlobalState gs = TestConfiguration.initState(spContext);
    WebState ws = gs.lookupWeb(TestConfiguration.Site1_URL, spContext);
    SharepointClient spclient = new SharepointClient(clientFactory, spContext);

    // Traverse the lists for the given web state
    spclient.traverse(gs, ws, 50, true);

    // Since there are 4 lists, the third list being set as last crawled,
    // the total no. of lists visited should be 2
    assertEquals(2, spclient.getNoOfVisitedListStates());
  }

  /**
   * Test case for
   * {@link SharepointClient#fetchACLInBatches(SPDocumentList, WebState, GlobalState, int)}
   *
   * @throws SharepointException
   */
  public void testFetchACLInBatches() throws SharepointException {
    SharepointClientContext spContext = TestConfiguration.initContext();
    spContext.setBatchHint(Integer.MAX_VALUE);
    spContext.setAclBatchSizeFactor(2);
    spContext.setFetchACLInBatches(true);
    GlobalState gs = TestConfiguration.initState(spContext);
    WebState ws = gs.lookupWeb(TestConfiguration.Site1_URL, spContext);
    SharepointClient spclient = new SharepointClient(clientFactory, spContext);

    SPDocument doc = new SPDocument("122", TestConfiguration.Site1_List1_URL,
        Calendar.getInstance(), ActionType.ADD);

    doc.setSharepointClientContext(spContext);

    List<SPDocument> list = new ArrayList<SPDocument>();
    list.add(doc);
    SPDocumentList docList = new SPDocumentList(list, gs);

    // Test that whenever 1 document, the batchsize is set to 1 and the
    // method does return and does not run into infinite loop
    boolean result = spclient.fetchACLInBatches(docList, ws, gs, 2);
    assertTrue(result);

    // Negative test case with 0 documents
    List<SPDocument> list2 = new ArrayList<SPDocument>();
    SPDocumentList docList2 = new SPDocumentList(list2, gs);
    assertFalse(spclient.fetchACLInBatches(docList2, ws, gs, 2));
  }

  /**
   * @throws SharepointException
   */
  public void testHandleACLForDocumentsForNonACLCrawl()
      throws SharepointException {
    SharepointClientContext spContext = TestConfiguration.initContext();
    spContext.setPushAcls(false);

    SharepointClient spclient = new SharepointClient(clientFactory, spContext);

    GlobalState gs = TestConfiguration.initState(spContext);
    WebState ws = gs.lookupWeb(TestConfiguration.Site1_URL, spContext);

    // Test that when feeding ACLs is turned off, you still get true to
    // indicate docs need to be fed to GSA
    assertTrue(spclient.handleACLForDocuments(null, ws, gs, false));

    SPDocumentList docList = getDocList(spContext, gs);
    spContext.setPushAcls(true);

    // Should fetch ACL and return true to indicate success
    assertTrue(spclient.handleACLForDocuments(docList, ws, gs, false));

    // Should just return true without fetching ACLs
    assertTrue(spclient.handleACLForDocuments(docList, ws, gs, true));
  }

  /**
   * Returns a doc list for test cases
   *
   * @param spContext The context info
   * @param gs The global state holding all web states and list states
   * @return The doc list
   */
  private SPDocumentList getDocList(SharepointClientContext spContext,
      GlobalState gs) {
    SPDocument doc = new SPDocument("122", TestConfiguration.Site1_List1_URL,
        Calendar.getInstance(), ActionType.ADD);

    doc.setSharepointClientContext(spContext);

    List<SPDocument> list = new ArrayList<SPDocument>();
    list.add(doc);
    SPDocumentList docList = new SPDocumentList(list, gs);
    return docList;
  }

  public void testHandleCrawlQueueForList() throws SharepointException {
    SharepointClientContext spContext = getSharePointClientContext();
    SharepointClient spClient =
        new SharepointClient(spContext.getClientFactory(), spContext);
    GlobalState globalState = new GlobalState(
        spContext.getClientFactory(),"temp",FeedType.CONTENT_FEED);
    WebState dummyWebState = globalState.makeWebState(
        spContext, "http://sharepoint.example.com/defaul.aspx");

    ListState list1 = new ListState("{GUID_LIST_1}", "List1", "GenericList",
        Calendar.getInstance(), "List",
        "http://sharepoint.example.com/List1/AllItems.aspx", dummyWebState);
    dummyWebState.AddOrUpdateListStateInWebState(
        list1, Util.calendarToJoda(Calendar.getInstance()));
    ListState list2 = new ListState("{GUID_LIST_2}", "List2", "GenericList",
        Calendar.getInstance(), "List",
        "http://sharepoint.example.com/List2/AllItems.aspx", dummyWebState);
    dummyWebState.AddOrUpdateListStateInWebState(
        list2, Util.calendarToJoda(Calendar.getInstance()));

    List<SPDocument> list1CrawlQueue = new ArrayList<SPDocument>();
    SPDocument correctDocument = new SPDocument(
        "LIST_ITEM_1", "http://sharepoint.example.com/List1/DispForm.aspx?ID=1",
        Calendar.getInstance(), ActionType.ADD);
    correctDocument.setParentList(list1);
    list1CrawlQueue.add(correctDocument);
    SPDocument missingParentDocument = new SPDocument(
        "LIST_ITEM_2_MISSING_PARRENT",
        "http://sharepoint.example.com/List1/DispForm.aspx?ID=2",
        Calendar.getInstance(), ActionType.ADD);
    list1CrawlQueue.add(missingParentDocument);
    SPDocument parentMismatchDocument = new SPDocument(
        "LIST_ITEM_3_MISMATCH",
        "http://sharepoint.example.com/List2/DispForm.aspx?ID=1",
        Calendar.getInstance(), ActionType.ADD);
    parentMismatchDocument.setParentList(list2);
    list1CrawlQueue.add(parentMismatchDocument);
    list1.setCrawlQueue(list1CrawlQueue);

    SPDocumentList docList =
        spClient.handleCrawlQueueForList(globalState, dummyWebState, list1);
    assertNotNull(docList);
    // verify SPDocumentList does not contain mismatched document
    assertEquals(ImmutableList.of(
        correctDocument, missingParentDocument), docList.getDocuments());
    // verify List crawl queue does not contain mismatched document
    assertEquals(ImmutableList.of(
        correctDocument, missingParentDocument), list1.getCrawlQueue());
    // verify parentlist for missing parent document is set after
    // handleCrawlQueueForList call
    assertEquals(list1, missingParentDocument.getParentList());
  }

  public void testTraverseNoACLs() throws SharepointException {
    SharepointClientContext spContext = getSharePointClientContext();
    SharepointClient spClient =
        new SharepointClient(spContext.getClientFactory(), spContext);
    GlobalState globalState = new GlobalState(
        spContext.getClientFactory(),"temp",FeedType.CONTENT_FEED);
    WebState dummyWebState = globalState.makeWebState(
        spContext, "http://sharepoint.example.com/defaul.aspx");

    ListState list1 = new ListState("{GUID_LIST_1}", "List1", "GenericList",
        Calendar.getInstance(), "List",
        "http://sharepoint.example.com/List1/AllItems.aspx", dummyWebState);
    dummyWebState.AddOrUpdateListStateInWebState(
        list1, Util.calendarToJoda(Calendar.getInstance()));
    ListState list2 = new ListState("{GUID_LIST_2}", "List2", "GenericList",
        Calendar.getInstance(), "List",
        "http://sharepoint.example.com/List2/AllItems.aspx", dummyWebState);
    dummyWebState.AddOrUpdateListStateInWebState(
        list2, Util.calendarToJoda(Calendar.getInstance()));

    List<SPDocument> list1CrawlQueue = new ArrayList<SPDocument>();
    SPDocument document0 = new SPDocument(
        "LIST_ITEM_1", "http://sharepoint.example.com/List1/DispForm.aspx?ID=1",
        Calendar.getInstance(), ActionType.ADD);
    document0.setParentList(list1);
    list1CrawlQueue.add(document0);
    SPDocument document1 = new SPDocument(
        "LIST_ITEM_2",
        "http://sharepoint.example.com/List1/DispForm.aspx?ID=2",
        Calendar.getInstance(), ActionType.ADD);
    document1.setParentList(list1);
    list1CrawlQueue.add(document1);
    list1.setCrawlQueue(list1CrawlQueue);

    SPDocument document2 = new SPDocument("LIST_ITEM_1_LIST2",
        "http://sharepoint.example.com/List2/DispForm.aspx?ID=1",
        Calendar.getInstance(), ActionType.ADD);
    document2.setParentList(list2);
    List<SPDocument> list2CrawlQueue = new ArrayList<SPDocument>();
    list2CrawlQueue.add(document2);
    list2.setCrawlQueue(list2CrawlQueue);

    SPDocumentList traversalResult =
        spClient.traverse(globalState, dummyWebState, 0, true);

    assertNotNull(traversalResult);
    assertEquals(ImmutableList.of(
        document0, document1, document2), traversalResult.getDocuments());
    assertEquals(ImmutableList.of(document0, document1), list1.getCrawlQueue());
    assertEquals(ImmutableList.of(document2), list2.getCrawlQueue());
  }

  /** Returns SharepointClientContext with MockClientFactory for tests. */
  private SharepointClientContext getSharePointClientContext() {
    MockClientFactory mockClientFactory = new MockClientFactory() {
      @Override public int checkConnectivity(HttpMethodBase method,
          Credentials credentials) throws IOException {
        return 200;
      }
    };
    SharepointClientContext spContext =
        new SharepointClientContext(mockClientFactory);
    spContext.setIncludedURlList("http://sharepoint.example.com");
    spContext.setUsername("username");
    spContext.setPassword("password");
    spContext.setPushAcls(false);
    spContext.setBatchHint(500);
    return spContext;
  }
}
