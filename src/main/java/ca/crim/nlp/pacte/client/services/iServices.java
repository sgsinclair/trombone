package ca.crim.nlp.pacte.client.services;

public interface iServices {
    /**
     * Run the service with the preset parameters.
     * 
     * @return Unique ID of the execution for status update.
     */
    public String execute();

    /**
     * Check the status of a specific execution.
     * 
     * @return JSON containing the status of execution
     */
    public String checkStatus(String tsUUID);

    /**
     * Check the status of the last execution
     * 
     * @return
     */
    public String checkStatus();

    /**
     * Retrieve the service published documentation.
     * 
     * @return Null if no info, a json doc otherwise.
     */
    public String getInfo();
}
