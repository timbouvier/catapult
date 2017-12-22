package com.tbouvier.mesos.util;

import org.apache.mesos.Protos;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.tbouvier.mesos.scheduler.Protos.SchedulerConstraint;

/**
 * Created by bouviti on 4/28/17.
 */
public class SchedulerConstraintUtils {


    private static boolean resourceListContains(String name, List<Protos.Resource> resources)
    {
        for(Protos.Resource resource : resources)
        {
            if( !resource.hasName() )
            {
                continue;
            }

            if( resource.getName().equals( name ) )
            {
                return true;
            }
        }

        return false;
    }

    private static Protos.Resource getResource(String name, List<Protos.Resource> resources)
    {
        for(Protos.Resource resource : resources)
        {
            if( !resource.hasName())
            {
                continue;
            }

            if( resource.getName().equals(name))
            {
                return resource;
            }
        }

        //FIXME: exception?
        return null;
    }

    private static void addResource(List<Protos.Resource> resources, Protos.Resource resource)
    {
        Protos.Resource op = getResource(resource.getName(), resources);

        try {
            //add the input resource to the same resource type in resources
            op = op.toBuilder().setScalar(Protos.Value.Scalar.newBuilder()
                    .setValue(op.toBuilder().getScalar().getValue() + resource.getScalar().getValue())
                    .build())
                    .build();
        }catch (NullPointerException e)
        {
            //?
        }
    }

    private static List<Protos.Resource> enumerateResources(List<Protos.Resource> resources)
    {
        List<Protos.Resource> ret = new LinkedList<>();

        for(Protos.Resource resource : resources)
        {
            //only process scalar named resources
            if( !resource.hasName() || !resource.hasScalar())

            if( resourceListContains(resource.getName(), ret) )
            {
                addResource(ret, resource);
            }
            else
            {
                ret.add( resource );
            }
        }

        return ret;
    }

    //loop through resources needed for task and make sure the offer has enough for each type
    private static boolean resourcesAvailable(Protos.Offer offer, SchedulerTask task)
    {
        Protos.TaskInfo taskInfo = task.getTaskInfo();
        List<Protos.Resource> enumeratedResources = enumerateResources(offer.getResourcesList());

        for(Protos.Resource resource : task.getTaskInfo().getResourcesList())
        {
            //only process scalar named resources
            if( !resource.hasName() || !resource.hasScalar() )
            {
                continue;
            }

            for(Protos.Resource enumeratedResource : enumeratedResources)
            {
                if( enumeratedResource.getName().equals(resource.getName()))
                {
                    if( enumeratedResource.getScalar().getValue() < resource.getScalar().getValue())
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Protos.Attribute getAttribute(String name, Protos.Offer offer)
    {
        for(Protos.Attribute attribute : offer.getAttributesList())
        {
            if( attribute.hasName() && attribute.getName().equals(name))
            {
                return attribute;
            }
        }

        return null;
    }

    //FIXME: support other attribute types besides Text
    private static boolean handleAttributeLike(SchedulerConstraint constraint, Protos.Offer offer)
    {
        Protos.Attribute attribute;

        if( (attribute = getAttribute(constraint.getAttribute(), offer)) == null )
        {
            return false;
        }

        if( attribute.hasText() && attribute.getText().getValue().equals(constraint.getValue()) )
        {
            return true;
        }

        return false;
    }

    private static boolean handleAttributeNotLike(SchedulerConstraint constraint, Protos.Offer offer)
    {
        Protos.Attribute attribute;

        if( (attribute = getAttribute(constraint.getAttribute(), offer)) == null )
        {
            return false;
        }

        if( attribute.hasText() && !attribute.getText().getValue().equals(constraint.getValue()) )
        {
            return true;
        }

        return false;
    }

    private static boolean constraintMet(SchedulerConstraint constraint, Protos.Offer offer)
    {
        switch (constraint.getType())
        {
            case RUN_ON_HOST_NO:      return !constraint.getValue().equals(offer.getHostname());
            case RUN_ON_HOST_YES:     return constraint.getValue().equals(offer.getHostname());
            case RUN_ON_SLAVE_NO:     return !constraint.getValue().equals(offer.getSlaveId().getValue());
            case RUN_ON_SLAVE_YES:    return constraint.getValue().equals(offer.getSlaveId().getValue());
            case ATTRIBUTE_LIKE:      return handleAttributeLike(constraint, offer);
            case ATTRIBUTE_NOT_LIKE:  return handleAttributeNotLike(constraint, offer);
            default:                  return false;
        }

    }

    public static boolean isRunnable(Protos.Offer offer, SchedulerTask task)
    {
        if( !resourcesAvailable(offer, task) )
        {
            return false;
        }

        for(SchedulerConstraint constraint : task.getConstraintsList())
        {
            if( !constraintMet(constraint, offer) )
            {
                return false;
            }
        }

        return true;
    }
}
