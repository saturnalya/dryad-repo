package org.dspace.workflow.actions.processingaction;

import com.atmire.authority.util.LoadCustomerCredit;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.*;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * User: lantian @ atmire . com
 * Date: 7/28/14
 * Time: 4:26 PM
 */
public class ReAuthorizationCreditAction extends ProcessingAction  {
    private static Logger log = Logger.getLogger(DryadReviewAction.class);



    @Override
    public void activate(Context c, WorkflowItem wfItem) {
        boolean test=true;
        log.debug("here");
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {

        try{
            Item item = wfi.getItem();
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c,item.getID());
            // if journal-based subscription is in place, transaction is paid
            if(!shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)&&shoppingCart.getJournalSub()) {
                log.info("processed journal subscription for Item " + item.getHandle() + ", journal = " + shoppingCart.getJournal());
                log.debug("deduct credit from journal = "+shoppingCart.getJournal());
                boolean success = false;
                Scheme scheme = Scheme.findByIdentifier(c, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
                Concept[] concepts = Concept.findByPreferredLabel(c,shoppingCart.getJournal(),scheme.getID());
                if(concepts!=null&&concepts.length!=0){
                    AuthorityMetadataValue[] metadataValues = concepts[0].getMetadata("internal", "journal", "customerId", Item.ANY);
                    if(metadataValues!=null&&metadataValues.length>0){
                        success = LoadCustomerCredit.updateCredit(metadataValues[0].value);
                        if(success)
                        {
                            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                            Date date= new Date();
                            shoppingCart.setPaymentDate(date);
                            shoppingCart.update();
                            sendPaymentApprovedEmail(c, wfi, shoppingCart);
                            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
                        }
                        else
                        {
                            sendPaymentErrorEmail(c, wfi, shoppingCart, "problem: credit not deducted successfully");
                        }
                    }
                }
            }
            else if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


        } catch (Exception e){

        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);

    }


    private void sendPaymentApprovedEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart) {

        try {

            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_approved"));
            email.addRecipient(wfi.getSubmitter().getEmail());
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.alert.recipient"));

            email.addArgument(
                    wfi.getItem().getName()
            );

            email.addArgument(
                    wfi.getSubmitter().getFullName() + " ("  +
                            wfi.getSubmitter().getEmail() + ")");

            if(shoppingCart != null)
            {
                /** add details of shopping cart */
                PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
                email.addArgument(paymentSystemService.printShoppingCart(c, shoppingCart));
            }

            email.send();

        } catch (Exception e) {
            log.error(LogManager.getHeader(c, "Error sending payment approved submission email", "WorkflowItemId: " + wfi.getID()), e);
        }

    }
    private void sendPaymentErrorEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart, String error) {

        try {

            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_error"));
            // only send result of shopping cart errors to administrators
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.alert.recipient"));

            email.addArgument(
                    wfi.getItem().getName()
            );

            email.addArgument(
                    wfi.getSubmitter().getFullName() + " ("  +
                            wfi.getSubmitter().getEmail() + ")");

            email.addArgument(error);

            if(shoppingCart != null)
            {
                /** add details of shopping cart */
                PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
                email.addArgument(paymentSystemService.printShoppingCart(c, shoppingCart));
            }

            email.send();

        } catch (Exception e) {
            log.error(LogManager.getHeader(c, "Error sending payment rejected submission email", "WorkflowItemId: " + wfi.getID()), e);
        }

    }

}