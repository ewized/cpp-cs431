public class ShortestRemainingTimeNextScheduler extends Scheduler
{
    public ShortestRemainingTimeNextScheduler(Processor theCPU)
    {
        super(theCPU);
    }

    public String algName() { return "Shortest Remaining Time Next Scheduler"; }


    Queue compare = new Queue();

    // This function does the scheduling work!
    public void tick()
    {
        // You can call job.timeLeft() to see how much time is left for this job
        Job job;

        for (job = cpu.unblockedQueue.first(); job != null; job = cpu.unblockedQueue.first()) {
            job.enqueueEnd(compare);
        }

        // If there are any new jobs being added to the processor, move them
        // from the newJobQueue to the end of the readyQueue.
        for (job = cpu.newJobQueue.first(); job != null; job = cpu.newJobQueue.first()) {
            job.enqueueEnd(compare);
        }

        // Check the job in the cpu (if any).  If it is done, move it to the done queue,
        // and if it is blocked, move it to the blocked queue.
        job = cpu.currentJob.first();
        if (job != null) {
            if (job.done()) {
                job.enqueueEnd(cpu.doneQueue);
            } else if (job.blocked()) {
                job.enqueueEnd(cpu.blockedQueue);
            }

            // swap out the jobs if needed
            for (Job compareJob = compare.first(); compareJob != null; compareJob = compareJob.next()) {
                if (!compare.empty()) {
                    if (job.estimatedLength() > compare.first().estimatedLength()) {
                        job.enqueueEnd(cpu.readyQueue);
                        compare.first().enqueueStart(cpu.currentJob);
                    } else {
                        compare.first().enqueueEnd(cpu.readyQueue);
                    }
                }

            }
        } else {
            if (!compare.empty()) {
                compare.first().enqueueEnd(cpu.readyQueue);
            }
        }


        // If the CPU is ready for a new job, start that job
        if (cpu.currentJob.empty()) {
            // get the least time first job
            job = cpu.readyQueue.first();
            for (Job a = cpu.readyQueue.first(); a != null; a = a.next()) {
                if (job.timeLeft() > a.timeLeft()) {
                    job = a;
                }
            }

            if (job != null) {
                job.enqueueStart(cpu.currentJob);
            }
        }

    }

    public void testCase1(Processor cpu)
    {
        cpu.addJob(1, 1, 50, "10,20,30,40", 1, 138);
        cpu.addJob(2, 1, 30, "7,15,22", 3, 83);
        cpu.addJob(3, 1, 20, "6,14", 2, 31);
        cpu.addJob(4, 1, 20, "6,14", 3, 50);
    }

    public void testCase2(Processor cpu)
    {
        cpu.addJob(1, 0, 30, "9,18,27", 4, 59);
        cpu.addJob(2, 1, 20, "3,6,9,12,15", 5, 46);
        cpu.addJob(3, 2, 30, "5,10,15,20,25", 2, 187);
        cpu.addJob(4, 21, 21, "20", 4, 78);
        cpu.addJob(5, 42, 29, "28", 3, 204);
        cpu.addJob(6, 71, 18, "5,10,15", 3, 106);
        cpu.addJob(7, 89, 13, "4,8,12", 4, 124);
        cpu.addJob(8, 102, 19, "4,8,12,16", 2, 148);
        cpu.addJob(9, 121, 19, "9,18", 1, 162);
        cpu.addJob(10, 140, 26, "6,12,18,24", 1, 245);
    }
}
