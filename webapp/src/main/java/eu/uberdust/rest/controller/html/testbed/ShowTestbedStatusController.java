package eu.uberdust.rest.controller.html.testbed;

import eu.uberdust.caching.Loggable;
import eu.uberdust.command.TestbedCommand;
import eu.uberdust.formatter.HtmlFormatter;
import eu.uberdust.formatter.exception.NotImplementedException;
import eu.uberdust.rest.exception.InvalidTestbedIdException;
import eu.uberdust.rest.exception.TestbedNotFoundException;
import eu.wisebed.wisedb.controller.LinkCapabilityController;
import eu.wisebed.wisedb.controller.NodeCapabilityController;
import eu.wisebed.wisedb.controller.TestbedController;
import eu.wisebed.wisedb.model.LinkCapability;
import eu.wisebed.wisedb.model.NodeCapability;
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
 * Controller class that returns the status page for the nodes and links of a testbed.
 */
public final class ShowTestbedStatusController extends AbstractRestController {

    /**
     * Testbed persistence manager.
     */
    private transient TestbedController testbedManager;

    /**
     * Last node reading persistence manager.
     */
    private transient NodeCapabilityController nodeCapabilityManager;

    /**
     * Last link reading persistence manager.
     */
    private transient LinkCapabilityController linkCapabilityManager;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ShowTestbedStatusController.class);

    /**
     * Constructor.
     */
    public ShowTestbedStatusController() {
        super();

        // Make sure to set which method this controller will support.
        this.setSupportedMethods(new String[]{METHOD_GET});
    }

    /**
     * Sets testbed persistence manager.
     *
     * @param testbedManager testbed persistence manager.
     */
    public void setTestbedManager(final TestbedController testbedManager) {
        this.testbedManager = testbedManager;
    }

    public void setNodeCapabilityManager(final NodeCapabilityController nodeCapabilityManager) {
        this.nodeCapabilityManager = nodeCapabilityManager;
    }

    public void setLinkCapabilityManager(final LinkCapabilityController linkCapabilityManager) {
        this.linkCapabilityManager = linkCapabilityManager;
    }

    /**
     * Handle request and return the appropriate response.
     *
     * @param request    http servlet request.
     * @param response   http servlet response.
     * @param commandObj command object.
     * @param errors     a BindException exception.
     * @return http servlet response.
     * @throws InvalidTestbedIdException a InvalidTestbedIDException exception.
     * @throws TestbedNotFoundException  a TestbedNotFoundException exception.
     */
    @Loggable
    protected ModelAndView handle(final HttpServletRequest request, final HttpServletResponse response,
                                  final Object commandObj, final BindException errors)
            throws InvalidTestbedIdException, TestbedNotFoundException {
        final long start = System.currentTimeMillis();

        long start1 = System.currentTimeMillis();

        // set command object
        final TestbedCommand command = (TestbedCommand) commandObj;

        // a specific testbed is requested by testbed Id
        int testbedId;
        try {
            testbedId = Integer.parseInt(command.getTestbedId());
        } catch (NumberFormatException nfe) {
            throw new InvalidTestbedIdException("Testbed IDs have number format.", nfe);
        }

        LOGGER.info("--------- Get Testbed id: " + (System.currentTimeMillis() - start1));
        start1 = System.currentTimeMillis();

        // look up testbed
        final Testbed testbed = testbedManager.getByID(testbedId);
        if (testbed == null) {
            // if no testbed is found throw exception
            throw new TestbedNotFoundException("Cannot find testbed [" + testbedId + "].");
        }
        LOGGER.info("got testbed " + testbed);

        LOGGER.info("--------- Get Testbed: " + (System.currentTimeMillis() - start1));


        if (nodeCapabilityManager == null) {
            LOGGER.error("nodeCapabilityManager==null");
        }

        start1 = System.currentTimeMillis();
        // get a list of node last readings from testbed
        final List<NodeCapability> nodeCapabilities = nodeCapabilityManager.list(testbed.getSetup());
        LOGGER.info("--------- list nodeCapabilities: " + (System.currentTimeMillis() - start1));

        start1 = System.currentTimeMillis();
        String nodeCaps;
        try {
            nodeCaps = HtmlFormatter.getInstance().formatLastNodeReadings(nodeCapabilities);
        } catch (NotImplementedException e) {
            nodeCaps = "";
        }
        LOGGER.info("--------- format last node readings: " + (System.currentTimeMillis() - start1));

        start1 = System.currentTimeMillis();
        // get a list of link statistics from testbed
        final List<LinkCapability> linkCapabilities = linkCapabilityManager.list(testbed.getSetup());
        LOGGER.info("--------- List link capabilities: " + (System.currentTimeMillis() - start1));


        // Prepare data to pass to jsp
        final Map<String, Object> refData = new HashMap<String, Object>();
        refData.put("testbed", testbed);
        refData.put("lastNodeReadings", nodeCaps);


        try {
            start1 = System.currentTimeMillis();
            refData.put("lastLinkReadings", HtmlFormatter.getInstance().formatLastLinkReadings(linkCapabilities));
            LOGGER.info("--------- format link Capabilites: " + (System.currentTimeMillis() - start1));
        } catch (NotImplementedException e) {
            LOGGER.error(e);
        }

        LOGGER.info("--------- Total time: " + (System.currentTimeMillis() - start));
        refData.put("time", String.valueOf((System.currentTimeMillis() - start)));
        LOGGER.info("prepared map");

        return new ModelAndView("testbed/status.html", refData);

    }
}
