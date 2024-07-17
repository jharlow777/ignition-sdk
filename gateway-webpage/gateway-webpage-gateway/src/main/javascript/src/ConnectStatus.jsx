import React, { useState } from 'react';
import { pollWaitAck } from 'ignition-lib';
import { modelInstall } from './model';
import Select from 'react-select';

const ConnectOverview = (props) => {
    const options = [
        { value: "Track And Trace", label: "Track And Trace" },
        { value: "Quality", label: "Quality" },
        { value: "Doc Management", label: "Doc Management" }
    ];

    const [features, setFeatures] = useState([]);

    const handleChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
    };

    const install = () => {
        console.log("Selected features: ");
        console.log(features.map((a) => a.value));
        let response = pollWaitAck(props.dispatch, modelInstall, 5000);
        console.log(response);
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