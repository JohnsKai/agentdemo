package com.study.agent.agentcore.model;

import java.util.Map;

public class ActionResult {
    private final ActionStatus status;
    private final Object output;
    private final String waitKey;
    private final Map<String, Object> observations;

    private final Throwable error;

    private ActionResult(ActionStatus status, Object output, String waitKey, Map<String, Object> observations, Throwable error) {
        this.status = status;
        this.output = output;
        this.waitKey = waitKey;
        this.observations = observations;
        this.error = error;
    }

    public static ActionResult success(Object output) {
        return new ActionResult(ActionStatus.SUCCESS, output, null, Map.of(), null);
    }

    public static ActionResult success(Object output, Map<String, Object> observations) {
        return new ActionResult(ActionStatus.SUCCESS, output, null, observations, null);
    }

    public static ActionResult waiting(String waitKey) {
        return new ActionResult(ActionStatus.WAITING, null, waitKey, Map.of(), null);
    }

    public static ActionResult failed(Throwable error) {
        return new ActionResult(ActionStatus.FAILED, error, null, Map.of(), error);
    }

    // 增加ask_user,增加过程中对用户提问的能力
    public static ActionResult askUser(String question, String waitKey) {
        return new ActionResult(ActionStatus.WAITING, null, waitKey, Map.of("question", question), null);
    }

    // getters
    public ActionStatus getStatus() {
        return status;
    }

    public Object getOutput() {
        return output;
    }

    public String getWaitKey() {
        return waitKey;
    }

    public Map<String, Object> getObservations() {
        return observations;
    }

    public Throwable getError() {
        return error;
    }
}
