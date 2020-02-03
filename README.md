# jep-dialogos

Experimental JEP-based plugin for DialogoS

## How to run

1. Get the current version of DialogOS from Github and `./gradlew publishToMavenLocal`
1. Set `PYTHONHOME` to wherever your Python lives (in my case, `/anaconda3/envs/pytorch13`)
2. Make the Jep library available (in my case, the easiest thing is `cp /anaconda3/envs/pytorch13/lib/python3.7/site-packages/jep/libjep.jnilib .`)
3. `./gradlew run`

