package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.paymentsystem.Voucher;
import org.dspace.paymentsystem.VoucherValidationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: lantian @ atmire . com
 * Date: 7/28/14
 * Time: 4:26 PM
 */
public class ReAuthorizationCreditActionXMLUI extends AbstractXMLUIAction {
    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String workflowID = request.getParameter("workflowID");
        String stepID = request.getParameter("stepID");
        String actionID = request.getParameter("actionID");

        Item item = workflowItem.getItem();
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        Collection collection = workflowItem.getCollection();

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";
        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");
        //generate form
        String errorMessage = request.getParameter("encountError");
        try{
            PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = payementSystemService.getShoppingCartByItemId(context,item.getID());
            if(errorMessage!=null&&errorMessage.length()>0)
            {
                mainDiv.addPara(errorMessage);
            }
            if(shoppingCart!=null&&!shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                mainDiv.addList("submit-credit").addItem().addButton("submit-credit").setValue("Re-submit Credit");
                mainDiv.addPara().addContent("NOTE : click the credit button will deduct your credit.");
            }
            else
            {
                mainDiv.addList("submit-next").addItem().addButton("submit-credit-next").setValue("Skip resubmit credit");
                mainDiv.addPara().addContent("NOTE : credit already deducted ,click next button to submit the item.");
            }


        }catch (Exception e)
        {
            //TODO: handle the exceptions
            log.error("Exception when entering the checkout step:", e);
        }
        mainDiv.addHidden("submission-continue").setValue(knot.getId());


    }
}
