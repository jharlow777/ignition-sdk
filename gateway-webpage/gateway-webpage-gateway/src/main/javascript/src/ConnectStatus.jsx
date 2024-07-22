import React, { useState } from 'react';
// import { pollWaitAck } from 'ignition-lib';
// import { modelInstall } from './model';
import Select from 'react-select';

const ConnectOverview = () => {
    const options = [
        { value: "TrackAndTrace", label: "Track And Trace" },
        { value: "Quality", label: "Quality" },
        { value: "DocumentManager", label: "Doc Management" }
    ];

    const [features, setFeatures] = useState([]);

    const handleChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
    };

    const install = () => {
        console.log("Selected features: ");
        let params = features.map((a) => a.value);
        console.log(params);

        // Convert params array to a query string
        let queryString = `?params=${params.join(',')}`;

        fetch(`/data/hce/install/${queryString}`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            console.log("JSON Response: ");
            console.log(json);
        });
    };

    return (
        <div>
            <div>
                <div>
                    <Select
                        options={options}
                        onChange={handleChange}
                        value={features}
                        isMulti
                    />
                    <button className="btn" id="btn" onClick={install}>
                        Install
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConnectOverview;