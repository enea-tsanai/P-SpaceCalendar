package eu.uberdust.rest.controller.html.link;

import eu.uberdust.caching.Loggable;
import eu.uberdust.command.LinkCommand;
import eu.uberdust.formatter.HtmlFormatter;
import eu.uberdust.formatter.exception.NotImplementedException;
import eu.uberdust.rest.exception.InvalidTestbedIdException;
import eu.uberdust.rest.exception.LinkNotFoundException;
import eu.uberdust.rest.exception.TestbedNotFoundException;
import eu.wisebed.wisedb.controller.LinkCapabilityController;
import eu.wisebed.wisedb.controller.LinkController;
import eu.wisebed.wisedb.controller.TestbedController;
import eu.wisebed.wisedb.model.Link;
import eu.wisebed.wisedb.model.LinkCapability;
import eu.wisebed.wisedb.model.Testbed;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractRestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller class that returns the a web page for a node.
 */
public final class ShowLinkController extends AbstractRestController {

    /**
     * Link persistence manager.
     */
    private transient LinkController linkManager;

    /**
     * Testbed persistence manager.
     */
    private transient TestbedController testbedManager;

    private transient LinkCapabilityController linkCapabilityManager;

    public void setLinkCapabilityManager(final LinkCapabilityController linkCapabilityManager) {
        this.linkCapabilityManager = linkCapabilityManager;
    }

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ShowLinkController.class);

    /**
     * Sets link persistence manager.
     *
     * @param linkManager link persistence manager.
     */
    public void setLinkManager(final LinkController linkManager) {
        this.linkManager = linkManager;
    }

    /**
     * Sets testbed persistence manager.
     *
     * @param testbedManager testbed persistence manager.
     */
    public void setTestbedManager(final TestbedController testbedManager) {
        this.testbedManager = testbedManager;
    }

    /**
     * Handle req and return the appropriate response.
     *
     * @param req        http servlet req.
     * @param response   http servlet response.
     * @param commandObj command object
     * @param errors     BindException exception.
     * @return http servlet response.
     * @throws InvalidTestbedIdException InvalidTestbedIdException exception.
     * @throws TestbedNotFoundException  TestbedNotFoundException exception.
     * @throws LinkNotFoundException     LinkNotFoundException exception.
     */
    @Loggable
    protected ModelAndView handle(final HttpServletRequest req, final HttpServletResponse response,
                                  final Object commandObj, final BindException errors)
            throws InvalidTestbedIdException, TestbedNotFoundException, LinkNotFoundException {

        HtmlFormatter.getInstance().setBaseUrl(req.getRequestURL().substring(0, req.getRequestURL().indexOf("/rest")));

        final long start = System.currentTimeMillis();

        // set command object
        final LinkCommand command = (LinkCommand) commandObj;

        // a specific testbed is requested by testbed Id
        int testbedId;
        try {
            testbedId = Integer.parseInt(command.getTestbedId());

        } catch (NumberFormatException nfe) {
            throw new InvalidTestbedIdException("Testbed IDs have number format.", nfe);
        }
        final Testbed testbed = testbedManager.getByID(Integer.parseInt(command.getTestbedId()));
        if (testbed == null) {
            // if no testbed is found throw exception
            throw new TestbedNotFoundException("Cannot find testbed [" + testbedId + "].");
        }

        // a link instance  and it' inverse
        final Link link = (command.getSourceId() == null || command.getTargetId() == null)
                ? null
                : linkManager.getByID(command.getSourceId(), command.getTargetId());
        final Link linkInv = (command.getSourceId() == null || command.getTargetId() == null)
                ? null
                : linkManager.getByID(command.getTargetId(), command.getSourceId());
        final Map<Link, List<LinkCapability>> linkCapabilityMap = new HashMap<Link, List<LinkCapability>>();

        // if no link or inverse link found return error view
        if (link == null && linkInv == null) {
            throw new LinkNotFoundException("Cannot find link [" + command.getSourceId() + "," + command.getTargetId()
                    + "] or the inverse link [" + command.getTargetId() + "," + command.getSourceId() + "]");
        }
        List<LinkCapability> linkCapabilities = linkCapabilityManager.list(link);

        // Prepare data to pass to jsp
        final Map<String, Object> refData = new HashMap<String, Object>();

        refData.put("testbed", testbed);
        refData.put("link", link);
        try {
            refData.put("text", HtmlFormatter.getInstance().formatLink(link));
        } catch (NotImplementedException e) {
            LOGGER.error(e);
        }
        try {
            refData.put("linkCapabilities", HtmlFormatter.getInstance().formatLinkCapabilities(linkCapabilities));
        } catch (NotImplementedException e) {
            LOGGER.error(e);
        }
        refData.put("time", String.valueOf((System.currentTimeMillis() - start)));

        return new ModelAndView("link/show.html", refData);
    }
}
